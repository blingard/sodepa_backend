package com.sodepa.erp.notification.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Écouteur AFTER_COMMIT publiant les événements Outbox de notification vers RabbitMQ uniquement
 * après validation durable de la transaction métier PostgreSQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher {

    private final NotificationOutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOutboxCreated(NotificationOutboxCreatedEvent event) {
        if (event == null || event.outboxId() == null) {
            return;
        }
        log.debug("[NOTIF-OUTBOX] Publication AFTER_COMMIT notificationId={}, canal={}",
                event.notificationId(), event.canal());
        outboxService.publishToBroker(event.outboxId());
    }
}
