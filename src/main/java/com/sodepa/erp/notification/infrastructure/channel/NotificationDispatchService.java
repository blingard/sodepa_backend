package com.sodepa.erp.notification.infrastructure.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.notification.application.NotificationDispatcher;
import com.sodepa.erp.notification.application.governance.NotificationPreferenceService;
import com.sodepa.erp.notification.application.governance.NotificationRuleService;
import com.sodepa.erp.notification.infrastructure.channel.email.EmailChannelAdapter;
import com.sodepa.erp.notification.infrastructure.channel.sms.SmsRegistry;
import com.sodepa.erp.notification.infrastructure.channel.whatsapp.WhatsAppRegistry;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import com.sodepa.erp.notification.infrastructure.metrics.NotificationMetrics;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationOutboxService;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrateur technique d'expédition asynchrone exécuté par RabbitMQ ou le rejeu ShedLock.
 * Assure l'idempotence, le repli WhatsApp -> SMS traçable, le suivi Outbox/DLQ et les métriques Micrometer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService implements NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxService outboxService;
    private final EmailChannelAdapter emailAdapter;
    private final SmsRegistry smsRegistry;
    private final WhatsAppRegistry whatsAppRegistry;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationRuleService ruleService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationPreferenceService preferenceService;

    @Override
    public void dispatch(UUID notificationId, UUID outboxId) {
        if (notificationId == null) {
            return;
        }
        NotificationEntity notif = notificationRepository.findById(notificationId).orElse(null);
        if (notif == null) {
            outboxService.markCompleted(outboxId);
            return;
        }
        if (isTerminalStatus(notif.getStatut())) {
            outboxService.markCompleted(outboxId);
            return;
        }

        long startNanos = System.nanoTime();
        NotificationChannelResult result = executeChannel(notif);
        long elapsedNanos = System.nanoTime() - startNanos;

        if (result.success()) {
            notif.setStatut(StatutNotification.SENT);
            notif.setProviderName(result.providerName());
            notif.setProviderMessageId(result.providerMessageId());
            notif.setFallbackUsed(notif.isFallbackUsed() || result.fallbackUsed());
            notif.setErreurTechnique(null);
            notif.setNextRetryAt(null);
            notificationRepository.save(notif);
            outboxService.markCompleted(outboxId);
            metrics.recordSent(notif.getCanal(), result.providerName(), elapsedNanos);
            return;
        }

        if (notif.getCanal() == CanalNotification.WHATSAPP && attemptWhatsAppFallbackToSms(notif, result)) {
            outboxService.markCompleted(outboxId);
            return;
        }

        metrics.recordFailed(notif.getCanal(), result.providerName(), elapsedNanos);
        outboxService.recordFailure(outboxId, notif.getId(), result.errorMessage(), true);
    }

    private NotificationChannelResult executeChannel(NotificationEntity notif) {
        return switch (notif.getCanal()) {
            case EMAIL -> emailAdapter.sendEmail(
                    notif.getDestinataireEmail(),
                    notif.getDestinataireNom(),
                    notif.getSujet(),
                    notif.getMessageCorps(),
                    notif.getMessageHtml()
            );
            case SMS -> smsRegistry.sendSms(
                    notif.getDestinataireTelephoneSms(),
                    buildSmsText(notif),
                    notif.getTypeEvenement()
            );
            case WHATSAPP -> dispatchWhatsApp(notif);
            case IN_APP -> NotificationChannelResult.ok("in_app", "INAPP-" + notif.getId());
            case EMAIL_ET_SMS -> NotificationChannelResult.failure(
                    "legacy",
                    "Canal combiné EMAIL_ET_SMS non expédiable directement.",
                    false
            );
        };
    }

    private NotificationChannelResult dispatchWhatsApp(NotificationEntity notif) {
        String phone = (notif.getDestinataireWhatsapp() != null && !notif.getDestinataireWhatsapp().isBlank())
                ? notif.getDestinataireWhatsapp()
                : notif.getDestinataireTelephoneSms();
        WhatsAppMeta meta = parseWhatsAppMeta(notif.getMetadataJson());
        return whatsAppRegistry.sendWhatsApp(
                phone,
                meta.templateName(),
                meta.language(),
                meta.params(),
                notif.getMessageCorps(),
                notif.getTypeEvenement()
        );
    }

    private boolean attemptWhatsAppFallbackToSms(NotificationEntity parentWa, NotificationChannelResult waFailure) {
        if (!properties.getWhatsapp().isFallbackToSmsOnFailure()) {
            return false;
        }
        String phone = (parentWa.getDestinataireTelephoneSms() != null && !parentWa.getDestinataireTelephoneSms().isBlank())
                ? parentWa.getDestinataireTelephoneSms()
                : parentWa.getDestinataireWhatsapp();
        if (phone == null || phone.isBlank()) {
            return false;
        }
        if (parentWa.getDestinataireUtilisateurId() != null && ruleService != null && preferenceService != null) {
            boolean criticalSms = ruleService.isEventCriticalOnChannel(parentWa.getTypeEvenement(), CanalNotification.SMS);
            var prefs = preferenceService.getPreferences(parentWa.getDestinataireUtilisateurId());
            if (!criticalSms && prefs != null && !prefs.isSmsEnabled()) {
                return false;
            }
        }

        String childKey = parentWa.getIdempotencyKey() != null
                ? parentWa.getIdempotencyKey() + ":fallback-sms"
                : "WA-FB-" + parentWa.getId();
        if (notificationRepository.findByIdempotencyKey(childKey).isPresent()) {
            return true;
        }

        long smsStart = System.nanoTime();
        NotificationChannelResult smsResult = smsRegistry.sendSms(phone, buildSmsText(parentWa), parentWa.getTypeEvenement());
        long smsElapsed = System.nanoTime() - smsStart;

        if (!smsResult.success()) {
            return false;
        }

        NotificationEntity smsChild = NotificationEntity.builder()
                .canal(CanalNotification.SMS)
                .groupId(parentWa.getGroupId())
                .parentNotificationId(parentWa.getId())
                .idempotencyKey(childKey)
                .destinataireUtilisateurId(parentWa.getDestinataireUtilisateurId())
                .destinataireNom(parentWa.getDestinataireNom())
                .destinataireTelephoneSms(phone)
                .sujet(parentWa.getSujet())
                .messageCorps(parentWa.getMessageCorps())
                .typeEvenement(parentWa.getTypeEvenement())
                .referenceEntiteType(parentWa.getReferenceEntiteType())
                .referenceEntiteId(parentWa.getReferenceEntiteId())
                .templateCode(parentWa.getTemplateCode())
                .statut(StatutNotification.SENT)
                .providerName(smsResult.providerName())
                .providerMessageId(smsResult.providerMessageId())
                .fallbackUsed(true)
                .dateEnvoi(LocalDateTime.now())
                .build();
        NotificationEntity savedChild = notificationRepository.save(smsChild);

        parentWa.setStatut(StatutNotification.FAILED);
        parentWa.setFallbackUsed(true);
        parentWa.setProviderName(waFailure.providerName());
        parentWa.setErreurTechnique(LogMasker.sanitizeError(
                "Bascule SMS exécutée (notificationId=" + savedChild.getId() + ") après échec WhatsApp: " + waFailure.errorMessage()
        ));
        parentWa.setNextRetryAt(null);
        notificationRepository.save(parentWa);

        metrics.recordFallback(CanalNotification.WHATSAPP, CanalNotification.SMS);
        metrics.recordSent(CanalNotification.SMS, smsResult.providerName(), smsElapsed);
        log.info("[NOTIF-FALLBACK] WhatsApp id={} basculé en SMS enfant id={} via provider={}",
                parentWa.getId(), savedChild.getId(), smsResult.providerName());
        return true;
    }

    private String buildSmsText(NotificationEntity notif) {
        String sujet = notif.getSujet() != null ? notif.getSujet().trim() : "";
        String corps = notif.getMessageCorps() != null ? notif.getMessageCorps().trim() : "";
        if (!sujet.isEmpty() && !corps.startsWith(sujet) && !corps.isEmpty()) {
            return sujet + " - " + corps;
        }
        return corps.isEmpty() ? sujet : corps;
    }

    private boolean isTerminalStatus(StatutNotification statut) {
        return statut == StatutNotification.SENT
                || statut == StatutNotification.SEND
                || statut == StatutNotification.READ
                || statut == StatutNotification.LUE
                || statut == StatutNotification.SKIPPED;
    }

    @SuppressWarnings("unchecked")
    private WhatsAppMeta parseWhatsAppMeta(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new WhatsAppMeta(null, "fr", Collections.emptyList());
        }
        try {
            Map<String, Object> map = objectMapper.readValue(metadataJson, Map.class);
            String templateName = map.get("whatsappTemplateName") != null ? map.get("whatsappTemplateName").toString() : null;
            String language = map.get("whatsappLanguage") != null ? map.get("whatsappLanguage").toString() : "fr";
            List<String> params = Collections.emptyList();
            if (map.get("whatsappParams") instanceof List<?> rawList) {
                params = rawList.stream().map(v -> v != null ? v.toString() : "").toList();
            }
            return new WhatsAppMeta(templateName, language, params);
        } catch (Exception e) {
            return new WhatsAppMeta(null, "fr", Collections.emptyList());
        }
    }

    private record WhatsAppMeta(String templateName, String language, List<String> params) {
    }
}
