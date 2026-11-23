package com.sodepa.erp.notification.application;

import java.util.UUID;

/**
 * Port interne d'expédition effective d'une notification (consommateur RabbitMQ ou rejeu immédiat).
 */
public interface NotificationDispatcher {

    /**
     * Exécute l'envoi technique d'une notification identifiée par son UUID et met à jour son outbox.
     */
    void dispatch(UUID notificationId, UUID outboxId);
}
