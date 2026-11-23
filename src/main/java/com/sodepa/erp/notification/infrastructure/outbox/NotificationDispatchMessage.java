package com.sodepa.erp.notification.infrastructure.outbox;

import java.util.UUID;

/**
 * Charge utile JSON publiée dans RabbitMQ pour déclencher l'envoi d'une notification persistée en base.
 */
public record NotificationDispatchMessage(
        UUID notificationId,
        UUID outboxId,
        String canal,
        String typeEvenement
) {
}
