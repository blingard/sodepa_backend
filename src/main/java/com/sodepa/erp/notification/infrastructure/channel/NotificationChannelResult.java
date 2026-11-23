package com.sodepa.erp.notification.infrastructure.channel;

/**
 * Résultat normalisé de l'appel à un adaptateur de canal (Email, SMS, WhatsApp, In-App).
 */
public record NotificationChannelResult(
        boolean success,
        boolean fallbackUsed,
        String providerName,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationChannelResult ok(String providerName, String providerMessageId) {
        return new NotificationChannelResult(true, false, providerName, providerMessageId, null);
    }

    public static NotificationChannelResult okWithFallback(String providerName, String providerMessageId, boolean fallbackUsed) {
        return new NotificationChannelResult(true, fallbackUsed, providerName, providerMessageId, null);
    }

    public static NotificationChannelResult failure(String providerName, String errorMessage, boolean fallbackUsed) {
        return new NotificationChannelResult(false, fallbackUsed, providerName, null, errorMessage);
    }
}
