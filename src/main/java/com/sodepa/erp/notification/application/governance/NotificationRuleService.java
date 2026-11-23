package com.sodepa.erp.notification.application.governance;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationRuleEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationUserPreferenceEntity;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRuleRepository;
import com.sodepa.erp.notification.presentation.dto.UpsertNotificationRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service d'évaluation et d'administration des règles de routage événement × canal.
 */
@Service
@RequiredArgsConstructor
public class NotificationRuleService {

    private static final Set<String> CORE_CRITICAL_EVENTS = Set.of(
            "IAM_DESACTIVATION_AUTOMATIQUE",
            "WORKFLOW_REJET_DOSSIER",
            "WORKFLOW_RELANCE_SLA"
    );

    private final NotificationRuleRepository ruleRepository;

    public List<NotificationRuleEntity> listAllRules() {
        return ruleRepository.findAllByOrderByTypeEvenementAscOrdrePrioriteAsc();
    }

    public List<NotificationRuleEntity> getRulesForEvent(String typeEvenement) {
        if (typeEvenement == null || typeEvenement.isBlank()) {
            return List.of();
        }
        return ruleRepository.findByTypeEvenementOrderByOrdrePrioriteAsc(normalizeEvent(typeEvenement));
    }

    @Transactional
    public NotificationRuleEntity upsertRule(UpsertNotificationRuleRequest req, String actorUsername) {
        if (req.canal() == CanalNotification.EMAIL_ET_SMS) {
            throw new IllegalArgumentException("Le canal composite EMAIL_ET_SMS ne peut pas être configuré en tant que règle unitaire.");
        }
        String event = normalizeEvent(req.typeEvenement());
        NotificationRuleEntity existing = ruleRepository.findByTypeEvenementAndCanal(event, req.canal()).orElse(null);

        boolean currentlyCritical = (existing != null && existing.isCritique()) || isCoreCriticalChannel(event, req.canal());
        if (currentlyCritical && !req.actif()) {
            throw new IllegalArgumentException(
                    "La règle critique (" + event + " / " + req.canal() + ") ne peut pas être désactivée."
            );
        }
        if (currentlyCritical && !req.critique()) {
            throw new IllegalArgumentException(
                    "Une règle métier critique (" + event + " / " + req.canal() + ") ne peut pas perdre son caractère critique."
            );
        }
        if (req.critique() && !req.actif()) {
            throw new IllegalArgumentException("Une règle marquée critique doit rester active.");
        }

        NotificationRuleEntity entity = existing != null ? existing : NotificationRuleEntity.builder()
                .typeEvenement(event)
                .canal(req.canal())
                .build();
        entity.setActif(req.actif());
        entity.setCritique(req.critique() || currentlyCritical);
        entity.setOrdrePriorite(req.ordrePriorite() != null ? req.ordrePriorite() : 10);
        entity.setFallbackCanal(req.fallbackCanal());
        entity.setDescription(req.description());
        entity.setUpdatedBy(actorUsername != null && !actorUsername.isBlank() ? actorUsername : "system");
        return ruleRepository.save(entity);
    }

    public boolean isEventCriticalOnChannel(String typeEvenement, CanalNotification canal) {
        String event = normalizeEvent(typeEvenement);
        if (isCoreCriticalChannel(event, canal)) {
            return true;
        }
        return ruleRepository.findByTypeEvenementAndCanal(event, canal)
                .map(NotificationRuleEntity::isCritique)
                .orElse(false);
    }

    /**
     * Résout les décisions par canal pour un événement métier et un utilisateur.
     *
     * @param includeAdditionalActiveRules {@code true} pour fan-out multi-canal piloté par les règles en base,
     *                                     {@code false} pour les appels unitaires mono-canal explicites.
     */
    public List<ChannelDecision> resolveChannelDecisions(
            String typeEvenement,
            Set<CanalNotification> requestedChannels,
            NotificationUserPreferenceEntity userPrefs,
            boolean hasUserAccount,
            boolean includeAdditionalActiveRules) {

        String event = normalizeEvent(typeEvenement);
        List<NotificationRuleEntity> dbRules = ruleRepository.findByTypeEvenementOrderByOrdrePrioriteAsc(event);
        Map<CanalNotification, NotificationRuleEntity> ruleByChannel = new LinkedHashMap<>();
        for (NotificationRuleEntity r : dbRules) {
            ruleByChannel.put(r.getCanal(), r);
        }

        Set<CanalNotification> expandedRequested = EnumSet.noneOf(CanalNotification.class);
        if (requestedChannels != null) {
            for (CanalNotification c : requestedChannels) {
                if (c == CanalNotification.EMAIL_ET_SMS) {
                    expandedRequested.add(CanalNotification.EMAIL);
                    expandedRequested.add(CanalNotification.SMS);
                } else if (c != null) {
                    expandedRequested.add(c);
                }
            }
        }

        Map<CanalNotification, NotificationRuleEntity> candidates = new LinkedHashMap<>();
        if (includeAdditionalActiveRules) {
            for (NotificationRuleEntity r : dbRules) {
                if (r.isActif() && r.getCanal() != CanalNotification.EMAIL_ET_SMS) {
                    candidates.put(r.getCanal(), r);
                }
            }
        }
        for (CanalNotification reqCanal : expandedRequested) {
            candidates.putIfAbsent(reqCanal, ruleByChannel.get(reqCanal));
        }

        List<ChannelDecision> decisions = new ArrayList<>();
        for (Map.Entry<CanalNotification, NotificationRuleEntity> entry : candidates.entrySet()) {
            CanalNotification canal = entry.getKey();
            if (canal == CanalNotification.IN_APP && !hasUserAccount) {
                continue;
            }
            NotificationRuleEntity rule = entry.getValue();
            boolean critical = (rule != null && rule.isCritique()) || isCoreCriticalChannel(event, canal);

            if (rule != null && !rule.isActif() && !critical) {
                decisions.add(new ChannelDecision(canal, false, false,
                        "Désactivé par règle d'administration (" + event + "/" + canal + ")", "RULE_DISABLED",
                        rule.getOrdrePriorite()));
                continue;
            }

            if (userPrefs != null && !critical && !isPreferenceEnabled(userPrefs, canal)) {
                decisions.add(new ChannelDecision(canal, false, false,
                        "Désactivé par préférence utilisateur (" + canal + ")", "USER_OPT_OUT",
                        rule != null ? rule.getOrdrePriorite() : 10));
                continue;
            }

            int order = rule != null ? rule.getOrdrePriorite() : 10;
            decisions.add(new ChannelDecision(canal, true, critical, null, null, order));
        }

        decisions.sort(Comparator.comparingInt(ChannelDecision::priorityOrder));
        return decisions;
    }

    private boolean isPreferenceEnabled(NotificationUserPreferenceEntity prefs, CanalNotification canal) {
        return switch (canal) {
            case EMAIL -> prefs.isEmailEnabled();
            case SMS -> prefs.isSmsEnabled();
            case WHATSAPP -> prefs.isWhatsappEnabled();
            case IN_APP -> prefs.isInAppEnabled();
            case EMAIL_ET_SMS -> prefs.isEmailEnabled() || prefs.isSmsEnabled();
        };
    }

    private boolean isCoreCriticalChannel(String event, CanalNotification canal) {
        return CORE_CRITICAL_EVENTS.contains(event)
                && (canal == CanalNotification.EMAIL || canal == CanalNotification.IN_APP || canal == CanalNotification.SMS);
    }

    private String normalizeEvent(String typeEvenement) {
        return typeEvenement != null ? typeEvenement.trim().toUpperCase(Locale.ROOT) : "NOTIFICATION_GENERALE";
    }

    public record ChannelDecision(
            CanalNotification canal,
            boolean enabled,
            boolean critical,
            String skipReason,
            String skipCode,
            int priorityOrder
    ) {
    }
}
