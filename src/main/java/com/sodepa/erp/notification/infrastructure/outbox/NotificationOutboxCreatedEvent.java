package com.sodepa.erp.notification.infrastructure.outbox;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;

import java.util.UUID;

/**
 * Événement Spring interne déclenché dans la transaction métier et publié vers RabbitMQ en phase AFTER_COMMIT.
 */
public record NotificationOutboxCreatedEvent(
        UUID notificationId,
        UUID outboxId,
        CanalNotification canal,
        String typeEvenement
) {
}
