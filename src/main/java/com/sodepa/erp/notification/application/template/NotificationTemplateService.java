package com.sodepa.erp.notification.application.template;

import com.sodepa.erp.notification.infrastructure.entities.NotificationTemplateEntity;
import com.sodepa.erp.notification.infrastructure.repo.NotificationTemplateRepository;
import com.sodepa.erp.notification.presentation.dto.PreviewNotificationTemplateRequest;
import com.sodepa.erp.notification.presentation.dto.UpsertNotificationTemplateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service d'administration et de rendu des modèles de notification persistés en base de données.
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final SafeHtmlTemplateEngine templateEngine;

    public List<NotificationTemplateEntity> listAllTemplates() {
        return templateRepository.findAllByOrderByTypeEvenementAscLangueAsc();
    }

    @Transactional
    public NotificationTemplateEntity upsertTemplate(UpsertNotificationTemplateRequest req, String actorUsername) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        String event = req.typeEvenement().trim().toUpperCase(Locale.ROOT);
        String langue = (req.langue() != null && !req.langue().isBlank())
                ? req.langue().trim().toLowerCase(Locale.ROOT)
                : "fr";

        templateEngine.validateTemplate(
                req.sujetTemplate(),
                req.bodyTextTemplate(),
                req.bodyHtmlTemplate(),
                req.allowedPlaceholders()
        );

        NotificationTemplateEntity entity = templateRepository.findByCode(code)
                .or(() -> templateRepository.findByTypeEvenementAndLangue(event, langue))
                .orElseGet(() -> NotificationTemplateEntity.builder()
                        .code(code)
                        .typeEvenement(event)
                        .langue(langue)
                        .build());

        entity.setCode(code);
        entity.setTypeEvenement(event);
        entity.setLangue(langue);
        entity.setSujetTemplate(req.sujetTemplate().trim());
        entity.setBodyTextTemplate(req.bodyTextTemplate());
        entity.setBodyHtmlTemplate(req.bodyHtmlTemplate());
        entity.setWhatsappTemplateName(req.whatsappTemplateName() != null ? req.whatsappTemplateName().trim() : null);
        entity.setWhatsappParamOrder(req.whatsappParamOrder() != null ? req.whatsappParamOrder().trim() : null);
        entity.setAllowedPlaceholders(req.allowedPlaceholders() != null ? req.allowedPlaceholders().trim() : null);
        entity.setActif(req.actif() == null || req.actif());
        entity.setUpdatedBy(actorUsername != null && !actorUsername.isBlank() ? actorUsername : "system");
        return templateRepository.save(entity);
    }

    public RenderedNotificationContent preview(PreviewNotificationTemplateRequest req) {
        String lookup = req.codeOrTypeEvenement().trim().toUpperCase(Locale.ROOT);
        String langue = (req.langue() != null && !req.langue().isBlank())
                ? req.langue().trim().toLowerCase(Locale.ROOT)
                : "fr";

        NotificationTemplateEntity template = templateRepository.findByCode(lookup)
                .or(() -> templateRepository.findByTypeEvenementAndLangue(lookup, langue))
                .or(() -> templateRepository.findByTypeEvenementAndLangue(lookup, "fr"))
                .orElseThrow(() -> new IllegalArgumentException("Modèle de notification introuvable : " + lookup));

        return renderTemplateEntity(template, req.variables());
    }

    /**
     * Résout et rend un modèle actif pour un événement et une langue, ou bascule sur les chaînes fournies
     * tout en générant un rendu HTML sûr si un modèle générique est disponible.
     */
    public RenderedNotificationContent renderForEvent(
            String typeEvenement,
            String langue,
            Map<String, String> variables,
            String fallbackSubject,
            String fallbackBody,
            boolean hasStructuredDomainVariables) {

        String event = typeEvenement != null ? typeEvenement.trim().toUpperCase(Locale.ROOT) : "NOTIFICATION_GENERALE";
        String lang = (langue != null && !langue.isBlank()) ? langue.trim().toLowerCase(Locale.ROOT) : "fr";

        NotificationTemplateEntity template = null;
        if (hasStructuredDomainVariables) {
            template = templateRepository.findByTypeEvenementAndLangueAndActifTrue(event, lang)
                    .or(() -> !"fr".equals(lang)
                            ? templateRepository.findByTypeEvenementAndLangueAndActifTrue(event, "fr")
                            : java.util.Optional.empty())
                    .orElse(null);
        }

        if (template != null) {
            return renderTemplateEntity(template, variables);
        }

        // Si le caller fournit déjà un texte métier (ou si aucun template structuré n'est applicable),
        // utiliser le wrapper HTML générique NOTIFICATION_GENERALE sans écraser le texte du caller.
        NotificationTemplateEntity generalTpl = templateRepository.findByTypeEvenementAndLangueAndActifTrue("NOTIFICATION_GENERALE", lang)
                .or(() -> templateRepository.findByTypeEvenementAndLangueAndActifTrue("NOTIFICATION_GENERALE", "fr"))
                .orElse(null);

        String effectiveSubject = (fallbackSubject != null && !fallbackSubject.isBlank()) ? fallbackSubject : "Notification ERP SODEPA";
        String effectiveBody = (fallbackBody != null && !fallbackBody.isBlank()) ? fallbackBody : effectiveSubject;

        if (generalTpl != null) {
            Map<String, String> generalVars = new java.util.LinkedHashMap<>();
            if (variables != null) {
                generalVars.putAll(variables);
            }
            generalVars.put("sujet", effectiveSubject);
            generalVars.put("message", effectiveBody);
            String html = templateEngine.renderHtml(generalTpl.getBodyHtmlTemplate(), generalVars, generalTpl.getAllowedPlaceholders());

            // Récupérer tout de même le mapping HSM WhatsApp du template événementiel si disponible
            NotificationTemplateEntity eventWaMeta = templateRepository.findByTypeEvenementAndLangueAndActifTrue(event, lang)
                    .or(() -> templateRepository.findByTypeEvenementAndLangueAndActifTrue(event, "fr"))
                    .orElse(generalTpl);
            List<String> waParams = templateEngine.extractOrderedWhatsAppParams(eventWaMeta.getWhatsappParamOrder(), generalVars);
            return new RenderedNotificationContent(
                    generalTpl.getCode(),
                    effectiveSubject,
                    effectiveBody,
                    html,
                    eventWaMeta.getWhatsappTemplateName(),
                    eventWaMeta.getLangue(),
                    waParams
            );
        }

        return new RenderedNotificationContent(null, effectiveSubject, effectiveBody, null, null, lang, List.of());
    }

    private RenderedNotificationContent renderTemplateEntity(NotificationTemplateEntity template, Map<String, String> variables) {
        String subject = templateEngine.renderPlain(template.getSujetTemplate(), variables, template.getAllowedPlaceholders());
        String text = templateEngine.renderPlain(template.getBodyTextTemplate(), variables, template.getAllowedPlaceholders());
        String html = templateEngine.renderHtml(template.getBodyHtmlTemplate(), variables, template.getAllowedPlaceholders());
        List<String> waParams = templateEngine.extractOrderedWhatsAppParams(template.getWhatsappParamOrder(), variables);
        return new RenderedNotificationContent(
                template.getCode(),
                subject,
                text,
                html,
                template.getWhatsappTemplateName(),
                template.getLangue(),
                waParams
        );
    }
}
