package com.sodepa.erp.notification.infrastructure.channel.whatsapp;

import java.util.List;

/**
 * Requête normalisée d'envoi WhatsApp Business (supporte HSM template + paramètres ordonnés et texte libre).
 */
public record WhatsAppDispatchRequest(
        String phoneE164,
        String templateName,
        String languageCode,
        List<String> templateParams,
        String fallbackText,
        String typeEvenement
) {
}
