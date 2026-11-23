package com.sodepa.erp.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.notification.application.governance.NotificationPreferenceService;
import com.sodepa.erp.notification.application.governance.NotificationRuleService;
import com.sodepa.erp.notification.application.template.NotificationTemplateService;
import com.sodepa.erp.notification.application.template.RenderedNotificationContent;
import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationUserPreferenceEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import com.sodepa.erp.notification.infrastructure.metrics.NotificationMetrics;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationOutboxService;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import com.sodepa.erp.notification.infrastructure.util.PhoneNumberNormalizer;
import com.sodepa.erp.notification.presentation.dto.DispatchNotificationRequest;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service central de gestion et d'orchestration des notifications (Email, SMS, WhatsApp, In-App) du socle ERP SODEPA.
 * <p>
 * Éclate chaque canal effectif en une ligne {@link NotificationEntity} dédiée partageant un {@code groupId},
 * évalue les règles événement × canal et préférences utilisateur, rend les modèles HTML sûrs, et délègue
 * l'expédition asynchrone à l'Outbox PostgreSQL + RabbitMQ.
 * </p>
 */
@Slf4j
@Service
public class NotificationService {

    private static final List<StatutNotification> IN_APP_UNREAD_STATUTS = List.of(
            StatutNotification.PENDING,
            StatutNotification.SENT,
            StatutNotification.SEND
    );

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxService outboxService;
    private final NotificationRuleService ruleService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateService templateService;
    private final NotificationMetrics metrics;
    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationOutboxService outboxService,
            NotificationRuleService ruleService,
            NotificationPreferenceService preferenceService,
            NotificationTemplateService templateService,
            NotificationMetrics metrics,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.outboxService = outboxService;
        this.ruleService = ruleService;
        this.preferenceService = preferenceService;
        this.templateService = templateService;
        this.metrics = metrics;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Constructeur minimal conservé pour la compatibilité des tests unitaires isolés sans contexte Spring complet.
     */
    @Deprecated(forRemoval = false)
    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, null, null, null, null, null, new ObjectMapper());
    }

    @Transactional
    public NotificationEntity envoyerEmail(
            UUID utilisateurId,
            String destinataireNom,
            String email,
            String sujet,
            String messageCorps,
            String typeEvenement,
            CanalNotification chanel) {
        CanalNotification requested = chanel != null ? chanel : CanalNotification.EMAIL;
        String event = normalizeEvent(typeEvenement, "NOTIFICATION_EMAIL");
        List<NotificationEntity> created = dispatchChannels(
                utilisateurId,
                destinataireNom,
                email,
                null,
                null,
                sujet,
                messageCorps,
                event,
                null,
                null,
                Set.of(requested),
                null,
                null,
                false,
                false
        );
        return created.get(0);
    }

    @Transactional
    public NotificationEntity envoyerEmail(
            UUID utilisateurId,
            String destinataireNom,
            String email,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {
        String event = normalizeEvent(typeEvenement, "NOTIFICATION_EMAIL");
        List<NotificationEntity> created = dispatchChannels(
                utilisateurId,
                destinataireNom,
                email,
                null,
                null,
                sujet,
                messageCorps,
                event,
                referenceEntiteType,
                referenceEntiteId,
                Set.of(CanalNotification.EMAIL),
                null,
                null,
                false,
                false
        );
        return created.get(0);
    }

    @Transactional
    public void sendNotificationList(Set<SendNotificationInput> utilisateurIds) {
        if (utilisateurIds == null || utilisateurIds.isEmpty()) {
            return;
        }
        for (SendNotificationInput input : utilisateurIds) {
            if (input == null) {
                continue;
            }
            CanalNotification requested = input.chanel() != null ? input.chanel() : CanalNotification.EMAIL_ET_SMS;
            String refId = input.workflowInstanceId() != null
                    ? input.workflowInstanceId().toString()
                    : input.workflowInstanceCodes();
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("destinataireNom", safeText(input.recipientName(), "Collaborateur"));
            vars.put("entityName", safeText(input.workflowInstanceCodes(), "WORKFLOW"));
            vars.put("entityId", safeText(refId, "-"));
            vars.put("etapeCode", "VALIDATION");
            vars.put("sujet", safeText(input.title(), "[ERP SODEPA] Notification Workflow"));
            vars.put("message", safeText(input.message(), input.title()));

            dispatchChannels(
                    input.recipientId(),
                    input.recipientName(),
                    input.recipientEmail(),
                    input.recipientPhone(),
                    input.recipientPhone(),
                    input.title(),
                    input.message(),
                    "WORKFLOW_ASSIGNATION_ETAPE",
                    "WORKFLOW_INSTANCE",
                    refId,
                    Set.of(requested),
                    vars,
                    null,
                    true,
                    false
            );
        }
    }

    @Transactional
    public NotificationEntity envoyerSms(
            UUID utilisateurId,
            String destinataireNom,
            String telephoneSms,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {
        String event = normalizeEvent(typeEvenement, "NOTIFICATION_SMS");
        List<NotificationEntity> created = dispatchChannels(
                utilisateurId,
                destinataireNom,
                null,
                telephoneSms,
                null,
                sujet,
                messageCorps,
                event,
                referenceEntiteType,
                referenceEntiteId,
                Set.of(CanalNotification.SMS),
                null,
                null,
                false,
                false
        );
        return created.get(0);
    }

    @Transactional
    public NotificationEntity envoyerWhatsApp(
            UUID utilisateurId,
            String destinataireNom,
            String whatsappPhone,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {
        String event = normalizeEvent(typeEvenement, "NOTIFICATION_WHATSAPP");
        List<NotificationEntity> created = dispatchChannels(
                utilisateurId,
                destinataireNom,
                null,
                whatsappPhone,
                whatsappPhone,
                sujet,
                messageCorps,
                event,
                referenceEntiteType,
                referenceEntiteId,
                Set.of(CanalNotification.WHATSAPP),
                null,
                null,
                false,
                false
        );
        return created.get(0);
    }

    @Transactional
    public List<NotificationEntity> dispatchMultiChannel(DispatchNotificationRequest req) {
        String event = normalizeEvent(req.typeEvenement(), "NOTIFICATION_GENERALE");
        Set<CanalNotification> requested = EnumSet.noneOf(CanalNotification.class);
        if (req.canaux() != null && !req.canaux().isEmpty()) {
            requested.addAll(req.canaux());
        } else {
            requested.add(CanalNotification.EMAIL);
            requested.add(CanalNotification.IN_APP);
        }
        boolean hasStructured = req.templateVariables() != null && !req.templateVariables().isEmpty();
        return dispatchChannels(
                req.utilisateurId(),
                req.destinataireNom(),
                req.email(),
                req.telephoneSms(),
                req.whatsappPhone(),
                req.sujet(),
                req.messageCorps(),
                event,
                req.referenceEntiteType(),
                req.referenceEntiteId(),
                requested,
                req.templateVariables(),
                req.idempotencyKey(),
                true,
                hasStructured
        );
    }

    @Transactional
    public NotificationEntity notifierEmailEtSms(
            UtilisateurEntity utilisateur,
            EmployeEntity employe,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId) {
        return notifierAvecVariables(
                utilisateur, employe, sujet, messageCorps, typeEvenement,
                referenceEntiteType, referenceEntiteId, null, false
        );
    }

    @Transactional
    public void notifierAssignationTacheWorkflow(
            UtilisateurEntity validateurUser,
            EmployeEntity validateurEmploye,
            String codeEtape,
            String libelleEtape,
            String entityName,
            String entityId,
            String consignesValidation) {
        String sujet = "[ERP SODEPA - Workflow] Action requise : Étape " + codeEtape;
        String corps = String.format(
                "Bonjour, une tâche de validation (%s) vous a été assignée sur le dossier %s [%s]. Exigences de l'étape : %s.",
                libelleEtape != null ? libelleEtape : codeEtape,
                entityName,
                entityId,
                consignesValidation != null ? consignesValidation : "Validation standard"
        );
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("etapeCode", safeText(codeEtape, "-"));
        vars.put("entityName", safeText(entityName, "-"));
        vars.put("entityId", safeText(entityId, "-"));
        vars.put("message", safeText(consignesValidation, "Validation standard"));

        notifierAvecVariables(
                validateurUser, validateurEmploye, sujet, corps,
                "WORKFLOW_ASSIGNATION_ETAPE", entityName, entityId, vars, true
        );
    }

    @Transactional
    public void notifierRejetDossierWorkflow(
            UtilisateurEntity initiateurUser,
            EmployeEntity initiateurEmploye,
            String codeEtape,
            String entityName,
            String entityId,
            String motifRejet) {
        String sujet = "[ERP SODEPA - Workflow] Dossier rejeté à l'étape " + codeEtape + " (" + entityName + ")";
        String corps = String.format(
                "Votre dossier %s [%s] a été rejeté/retourné à l'étape %s. Motif : %s.",
                entityName, entityId, codeEtape, motifRejet
        );
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("etapeCode", safeText(codeEtape, "-"));
        vars.put("entityName", safeText(entityName, "-"));
        vars.put("entityId", safeText(entityId, "-"));
        vars.put("motif", safeText(motifRejet, "Non précisé"));
        vars.put("message", corps);

        notifierAvecVariables(
                initiateurUser, initiateurEmploye, sujet, corps,
                "WORKFLOW_REJET_DOSSIER", entityName, entityId, vars, true
        );
    }

    @Transactional
    public void notifierClotureApprouveeWorkflow(
            UtilisateurEntity initiateurUser,
            EmployeEntity initiateurEmploye,
            String entityName,
            String entityId) {
        String sujet = "[ERP SODEPA - Workflow] Dossier approuvé et clôturé (" + entityName + " #" + entityId + ")";
        String corps = String.format(
                "Votre dossier %s [%s] a franchi avec succès toutes les étapes du circuit de validation et a été approuvé.",
                entityName, entityId
        );
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("entityName", safeText(entityName, "-"));
        vars.put("entityId", safeText(entityId, "-"));
        vars.put("message", corps);

        notifierAvecVariables(
                initiateurUser, initiateurEmploye, sujet, corps,
                "WORKFLOW_CLOTURE_APPROUVEE", entityName, entityId, vars, true
        );
    }

    @Transactional
    public void notifierExpirationPieceTiers(
            String codeTiers,
            String raisonSociale,
            String typePiece,
            String dateExpiration,
            String emailDestinataire,
            String telephoneDestinataire) {
        String sujet = "[ERP SODEPA - Conformité Tiers J-15] Expiration imminente : " + typePiece + " (" + codeTiers + ")";
        String corps = String.format(
                "Alerte de conformité (CA-REF-023-01) : La pièce %s du tiers %s [%s] expire le %s. Veuillez fournir une pièce à jour sous 15 jours pour éviter la suspension des règlements et commandes.",
                typePiece, raisonSociale, codeTiers, dateExpiration
        );
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("destinataireNom", safeText(raisonSociale, codeTiers));
        vars.put("typePiece", safeText(typePiece, "PIÈCE"));
        vars.put("dateExpiration", safeText(dateExpiration, "-"));
        vars.put("joursRestants", "15");
        vars.put("message", corps);

        Set<CanalNotification> channels = EnumSet.noneOf(CanalNotification.class);
        if (emailDestinataire != null && !emailDestinataire.isBlank()) {
            channels.add(CanalNotification.EMAIL);
        }
        if (telephoneDestinataire != null && !telephoneDestinataire.isBlank()) {
            channels.add(CanalNotification.SMS);
        }
        if (channels.isEmpty()) {
            channels.add(CanalNotification.EMAIL);
        }

        dispatchChannels(
                null,
                raisonSociale,
                emailDestinataire,
                telephoneDestinataire,
                telephoneDestinataire,
                sujet,
                corps,
                "TIERS_PIECE_EXPIRATION_J15",
                "TIERS_PIECE",
                codeTiers,
                channels,
                vars,
                null,
                true,
                true
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> listerNotificationsUtilisateur(UUID utilisateurId) {
        if (utilisateurId == null) {
            return List.of();
        }
        return notificationRepository.findByDestinataireUtilisateurIdOrderByDateEnvoiDesc(utilisateurId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> listerNotificationsUtilisateur(UUID utilisateurId, Pageable pageable) {
        if (utilisateurId == null) {
            return Page.empty(pageable);
        }
        return notificationRepository.findByDestinataireUtilisateurIdOrderByDateEnvoiDesc(utilisateurId, pageable);
    }

    /**
     * Retourne uniquement les notifications In-App non lues pour la cloche UI (aucune fuite de lignes Email/SMS/WhatsApp/Skipped).
     */
    @Transactional(readOnly = true)
    public List<NotificationEntity> listerNotificationsEnAttenteUtilisateur(UUID utilisateurId) {
        if (utilisateurId == null) {
            return List.of();
        }
        return notificationRepository.findByDestinataireUtilisateurIdAndCanalAndStatutInOrderByDateEnvoiDesc(
                utilisateurId, CanalNotification.IN_APP, IN_APP_UNREAD_STATUTS
        );
    }

    @Transactional
    public NotificationEntity marquerCommeLue(UUID notificationId) {
        NotificationEntity notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RessourceIntrouvableException("Notification introuvable : " + notificationId));
        notif.setStatut(StatutNotification.READ);
        notif.setDateLecture(LocalDateTime.now());
        return notificationRepository.save(notif);
    }

    @Transactional
    public NotificationEntity marquerCommeLue(UUID notificationId, UUID currentUserId, boolean isAdmin) {
        NotificationEntity notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RessourceIntrouvableException("Notification introuvable : " + notificationId));
        if (!isAdmin) {
            if (currentUserId == null || notif.getDestinataireUtilisateurId() == null
                    || !currentUserId.equals(notif.getDestinataireUtilisateurId())) {
                throw new AccesRefuseException("Vous ne pouvez acquitter que vos propres notifications.");
            }
        }
        notif.setStatut(StatutNotification.READ);
        notif.setDateLecture(LocalDateTime.now());
        return notificationRepository.save(notif);
    }

    private NotificationEntity notifierAvecVariables(
            UtilisateurEntity utilisateur,
            EmployeEntity employe,
            String sujet,
            String messageCorps,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId,
            Map<String, String> domainVars,
            boolean hasStructuredDomainVars) {

        UUID userId = utilisateur != null ? utilisateur.getId() : null;
        String nom = resolveRecipientName(utilisateur, employe);
        String email = resolveEmail(utilisateur, employe);
        String phone = resolvePhone(utilisateur, employe);

        Map<String, String> vars = new LinkedHashMap<>();
        if (domainVars != null) {
            vars.putAll(domainVars);
        }
        vars.putIfAbsent("destinataireNom", nom);
        vars.putIfAbsent("username", utilisateur != null && utilisateur.getUsername() != null ? utilisateur.getUsername() : nom);
        vars.putIfAbsent("sujet", safeText(sujet, "Notification ERP SODEPA"));
        vars.putIfAbsent("message", safeText(messageCorps, ""));

        List<NotificationEntity> created = dispatchChannels(
                userId,
                nom,
                email,
                phone,
                phone,
                sujet,
                messageCorps,
                normalizeEvent(typeEvenement, "NOTIFICATION_GENERALE"),
                referenceEntiteType,
                referenceEntiteId,
                EnumSet.of(CanalNotification.EMAIL, CanalNotification.SMS),
                vars,
                null,
                true,
                hasStructuredDomainVars
        );
        return created.get(0);
    }

    private List<NotificationEntity> dispatchChannels(
            UUID utilisateurId,
            String destinataireNom,
            String rawEmail,
            String rawPhoneSms,
            String rawWhatsappPhone,
            String fallbackSubject,
            String fallbackBody,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId,
            Collection<CanalNotification> requestedChannels,
            Map<String, String> extraVars,
            String baseIdempotencyKey,
            boolean includeAdditionalActiveRules,
            boolean hasStructuredDomainVars) {

        UUID groupId = UUID.randomUUID();
        NotificationUserPreferenceEntity userPrefs = (utilisateurId != null && preferenceService != null)
                ? preferenceService.getPreferences(utilisateurId)
                : null;
        String langue = (userPrefs != null && userPrefs.getLangue() != null) ? userPrefs.getLangue() : "fr";

        String effectiveWhatsappPhone = firstNonBlank(
                rawWhatsappPhone,
                userPrefs != null ? userPrefs.getWhatsappPhone() : null,
                rawPhoneSms
        );
        String normalizedSms = PhoneNumberNormalizer.normalize(rawPhoneSms, "+237");
        String normalizedWa = PhoneNumberNormalizer.normalize(effectiveWhatsappPhone, "+237");
        String safeNom = safeText(destinataireNom, "Collaborateur");

        Map<String, String> templateVars = new LinkedHashMap<>();
        if (extraVars != null) {
            templateVars.putAll(extraVars);
        }
        templateVars.putIfAbsent("destinataireNom", safeNom);
        templateVars.putIfAbsent("entityName", safeText(referenceEntiteType, "-"));
        templateVars.putIfAbsent("entityId", safeText(referenceEntiteId, "-"));
        templateVars.putIfAbsent("sujet", safeText(fallbackSubject, "Notification ERP SODEPA"));
        templateVars.putIfAbsent("message", safeText(fallbackBody, ""));

        RenderedNotificationContent rendered = templateService != null
                ? templateService.renderForEvent(typeEvenement, langue, templateVars, fallbackSubject, fallbackBody, hasStructuredDomainVars)
                : new RenderedNotificationContent(
                        null,
                        safeText(fallbackSubject, "Notification ERP SODEPA"),
                        safeText(fallbackBody, ""),
                        null, null, langue, List.of()
                );

        List<NotificationRuleService.ChannelDecision> decisions = resolveDecisions(
                typeEvenement, requestedChannels, userPrefs, utilisateurId != null, includeAdditionalActiveRules
        );

        String metadataJson = serializeMetadata(rendered);
        List<NotificationEntity> persisted = new ArrayList<>();

        for (NotificationRuleService.ChannelDecision decision : decisions) {
            CanalNotification canal = decision.canal();
            String idempotencyKey = buildChannelIdempotencyKey(baseIdempotencyKey, groupId, canal);

            if (baseIdempotencyKey != null && !baseIdempotencyKey.isBlank()) {
                var existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    persisted.add(existing.get());
                    continue;
                }
            }

            if (!decision.enabled()) {
                NotificationEntity skipped = buildBaseEntity(
                        groupId, canal, idempotencyKey, utilisateurId, safeNom,
                        rawEmail, normalizedSms, normalizedWa, rendered, typeEvenement,
                        referenceEntiteType, referenceEntiteId, metadataJson
                );
                skipped.setStatut(StatutNotification.SKIPPED);
                skipped.setProviderName(decision.skipCode());
                skipped.setErreurTechnique(decision.skipReason());
                persisted.add(notificationRepository.save(skipped));
                recordSkippedMetric(canal, decision.skipCode());
                continue;
            }

            if (!hasValidContact(canal, rawEmail, normalizedSms, normalizedWa, utilisateurId)) {
                NotificationEntity skipped = buildBaseEntity(
                        groupId, canal, idempotencyKey, utilisateurId, safeNom,
                        rawEmail, normalizedSms, normalizedWa, rendered, typeEvenement,
                        referenceEntiteType, referenceEntiteId, metadataJson
                );
                skipped.setStatut(StatutNotification.SKIPPED);
                skipped.setProviderName("MISSING_CONTACT");
                skipped.setErreurTechnique("Coordonnée manquante ou invalide pour le canal " + canal.name());
                persisted.add(notificationRepository.save(skipped));
                recordSkippedMetric(canal, "MISSING_CONTACT");
                log.debug("[NOTIFICATION-SKIP] canal={}, evt={}, motif=MISSING_CONTACT (email={}, tel={})",
                        canal, typeEvenement, LogMasker.maskEmail(rawEmail), LogMasker.maskPhone(rawPhoneSms));
                continue;
            }

            NotificationEntity entity = buildBaseEntity(
                    groupId, canal, idempotencyKey, utilisateurId, safeNom,
                    rawEmail, normalizedSms, normalizedWa, rendered, typeEvenement,
                    referenceEntiteType, referenceEntiteId, metadataJson
            );

            if (canal == CanalNotification.IN_APP) {
                entity.setStatut(StatutNotification.SENT);
                entity.setProviderName("in_app");
                NotificationEntity saved = notificationRepository.save(entity);
                persisted.add(saved);
                if (metrics != null) {
                    metrics.recordSent(CanalNotification.IN_APP, "in_app", 0L);
                }
            } else {
                entity.setStatut(StatutNotification.PENDING);
                NotificationEntity saved = notificationRepository.save(entity);
                persisted.add(saved);
                if (outboxService != null) {
                    outboxService.enqueue(saved);
                }
            }

            log.info("[NOTIFICATION-QUEUED] id={}, groupId={}, canal={}, evt={}, email={}, tel={}",
                    entity.getId(), groupId, canal, typeEvenement,
                    LogMasker.maskEmail(rawEmail), LogMasker.maskPhone(normalizedSms));
        }

        return persisted;
    }

    private List<NotificationRuleService.ChannelDecision> resolveDecisions(
            String typeEvenement,
            Collection<CanalNotification> requestedChannels,
            NotificationUserPreferenceEntity userPrefs,
            boolean hasUserAccount,
            boolean includeAdditionalActiveRules) {
        if (ruleService != null) {
            return ruleService.resolveChannelDecisions(
                    typeEvenement,
                    Set.copyOf(requestedChannels),
                    userPrefs,
                    hasUserAccount,
                    includeAdditionalActiveRules
            );
        }
        List<NotificationRuleService.ChannelDecision> defaults = new ArrayList<>();
        for (CanalNotification c : requestedChannels) {
            if (c == CanalNotification.EMAIL_ET_SMS) {
                defaults.add(new NotificationRuleService.ChannelDecision(CanalNotification.EMAIL, true, false, null, null, 10));
                defaults.add(new NotificationRuleService.ChannelDecision(CanalNotification.SMS, true, false, null, null, 20));
            } else if (c != null) {
                defaults.add(new NotificationRuleService.ChannelDecision(c, true, false, null, null, 10));
            }
        }
        return defaults;
    }

    private NotificationEntity buildBaseEntity(
            UUID groupId,
            CanalNotification canal,
            String idempotencyKey,
            UUID utilisateurId,
            String safeNom,
            String rawEmail,
            String normalizedSms,
            String normalizedWa,
            RenderedNotificationContent rendered,
            String typeEvenement,
            String referenceEntiteType,
            String referenceEntiteId,
            String metadataJson) {
        return NotificationEntity.builder()
                .canal(canal)
                .groupId(groupId)
                .idempotencyKey(idempotencyKey)
                .destinataireUtilisateurId(utilisateurId)
                .destinataireNom(safeNom)
                .destinataireEmail(canal == CanalNotification.EMAIL ? trimOrNull(rawEmail) : null)
                .destinataireTelephoneSms(canal == CanalNotification.SMS || canal == CanalNotification.WHATSAPP ? normalizedSms : null)
                .destinataireWhatsapp(canal == CanalNotification.WHATSAPP ? normalizedWa : null)
                .sujet(truncate(rendered.subject(), 250))
                .messageCorps(rendered.plainText())
                .messageHtml(canal == CanalNotification.EMAIL ? rendered.htmlBody() : null)
                .typeEvenement(typeEvenement)
                .referenceEntiteType(referenceEntiteType)
                .referenceEntiteId(referenceEntiteId)
                .templateCode(rendered.templateCode())
                .metadataJson(canal == CanalNotification.WHATSAPP ? metadataJson : null)
                .dateEnvoi(LocalDateTime.now())
                .build();
    }

    private boolean hasValidContact(CanalNotification canal, String email, String sms, String whatsapp, UUID userId) {
        return switch (canal) {
            case EMAIL -> email != null && !email.isBlank() && email.contains("@");
            case SMS -> sms != null && !sms.isBlank();
            case WHATSAPP -> whatsapp != null && !whatsapp.isBlank();
            case IN_APP -> userId != null;
            case EMAIL_ET_SMS -> false;
        };
    }

    private void recordSkippedMetric(CanalNotification canal, String reason) {
        if (metrics != null) {
            metrics.recordSkipped(canal, reason);
        }
    }

    private String buildChannelIdempotencyKey(String baseKey, UUID groupId, CanalNotification canal) {
        if (baseKey != null && !baseKey.isBlank()) {
            return baseKey.trim() + ":" + canal.name();
        }
        return groupId + ":" + canal.name();
    }

    private String serializeMetadata(RenderedNotificationContent rendered) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            if (rendered.whatsappTemplateName() != null) {
                meta.put("whatsappTemplateName", rendered.whatsappTemplateName());
            }
            if (rendered.whatsappLanguage() != null) {
                meta.put("whatsappLanguage", rendered.whatsappLanguage());
            }
            if (rendered.whatsappParams() != null && !rendered.whatsappParams().isEmpty()) {
                meta.put("whatsappParams", rendered.whatsappParams());
            }
            return meta.isEmpty() ? null : objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveRecipientName(UtilisateurEntity utilisateur, EmployeEntity employe) {
        if (employe != null && employe.getNom() != null) {
            return ((employe.getPrenom() != null ? employe.getPrenom() + " " : "") + employe.getNom()).trim();
        }
        if (utilisateur != null && utilisateur.getNom() != null) {
            return ((utilisateur.getPrenom() != null ? utilisateur.getPrenom() + " " : "") + utilisateur.getNom()).trim();
        }
        if (utilisateur != null && utilisateur.getUsername() != null) {
            return utilisateur.getUsername();
        }
        return "Collaborateur SODEPA";
    }

    private String resolveEmail(UtilisateurEntity utilisateur, EmployeEntity employe) {
        if (utilisateur != null && utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) {
            return utilisateur.getEmail().trim();
        }
        if (employe != null && employe.getEmail() != null && !employe.getEmail().isBlank()) {
            return employe.getEmail().trim();
        }
        return null;
    }

    private String resolvePhone(UtilisateurEntity utilisateur, EmployeEntity employe) {
        if (employe != null && employe.getTelephone() != null && !employe.getTelephone().isBlank()) {
            return employe.getTelephone().trim();
        }
        if (utilisateur != null && utilisateur.getTelephones() != null && !utilisateur.getTelephones().isEmpty()) {
            return utilisateur.getTelephones().stream()
                    .filter(p -> p != null && !p.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String normalizeEvent(String typeEvenement, String defaultValue) {
        return (typeEvenement == null || typeEvenement.isBlank())
                ? defaultValue
                : typeEvenement.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String safeText(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val.trim() : fallback;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String trimOrNull(String v) {
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    private String truncate(String val, int max) {
        if (val == null) {
            return "";
        }
        return val.length() > max ? val.substring(0, max) : val;
    }
}
