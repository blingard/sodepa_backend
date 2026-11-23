package com.sodepa.erp.notification.infrastructure.listener;

import com.sodepa.erp.configuration.RabbitMqConfig;
import com.sodepa.erp.notification.application.NotificationDispatcher;
import com.sodepa.erp.notification.infrastructure.outbox.NotificationDispatchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consommateur RabbitMQ déclenchant l'expédition multi-canal asynchrone des notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRabbitConsumer {

    private final NotificationDispatcher notificationDispatcher;

    @RabbitListener(queues = RabbitMqConfig.NOTIF_QUEUE)
    public void consume(NotificationDispatchMessage message) {
        if (message == null || message.notificationId() == null) {
            log.warn("[NOTIF-CONSUMER] Message RabbitMQ ignoré (identifiant de notification absent).");
            return;
        }
        log.debug("[NOTIF-CONSUMER] Traitement notificationId={}, canal={}", message.notificationId(), message.canal());
        notificationDispatcher.dispatch(message.notificationId(), message.outboxId());
    }
}
