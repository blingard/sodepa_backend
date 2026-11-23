package com.sodepa.erp.notification.application.template;

import java.util.List;

/**
 * Résultat du rendu sûr d'un modèle de notification.
 */
public record RenderedNotificationContent(
        String templateCode,
        String subject,
        String plainText,
        String htmlBody,
        String whatsappTemplateName,
        String whatsappLanguage,
        List<String> whatsappParams
) {
}
