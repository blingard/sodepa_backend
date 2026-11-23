package com.sodepa.erp.notification.infrastructure.channel.sms;

/**
 * Requête normalisée d'envoi d'un SMS CEMAC.
 */
public record SmsDispatchRequest(
        String phoneE164,
        String messageText,
        String typeEvenement
) {
}
