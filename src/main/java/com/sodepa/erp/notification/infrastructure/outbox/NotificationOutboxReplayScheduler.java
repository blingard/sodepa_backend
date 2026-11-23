package com.sodepa.erp.notification.infrastructure.outbox;

import com.sodepa.erp.notification.application.NotificationDispatcher;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.entities.NotificationOutboxEntity;
import com.sodepa.erp.notification.infrastructure.entities.OutboxStatus;
import com.sodepa.erp.notification.infrastructure.repo.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Planificateur protégé par ShedLock assurant le rejeu des notifications en attente ou en échec
 * temporaire avec backoff exponentiel borné et isolement DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxReplayScheduler {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxService outboxService;
    private final NotificationProperties properties;
    private final NotificationDispatcher dispatcher;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${sodepa.notifications.outbox-replay-ms:15000}")
    @SchedulerLock(name = "notificationOutboxReplay", lockAtMostFor = "PT2M", lockAtLeastFor = "PT5S")
    public void replayPendingNotifications() {
        int batchSize = Math.max(1, properties.getOutboxBatchSize());
        int unstuck = outboxService.requeueStalePublished(LocalDateTime.now().minusMinutes(5), batchSize);
        if (unstuck > 0) {
            log.info("[NOTIF-REPLAY] {} entrée(s) bloquée(s) en statut PUBLISHED remise(s) en file.", unstuck);
        }

        List<NotificationOutboxEntity> due = outboxRepository.findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );
        if (due.isEmpty()) {
            return;
        }

        int maxRetries = Math.max(1, properties.getMaxRetries());
        int replayed = 0;
        for (NotificationOutboxEntity row : due) {
            if (row.getRetryCount() >= maxRetries) {
                outboxService.recordFailure(row.getId(), row.getNotificationId(),
                        "Nombre maximal de tentatives atteint (" + row.getRetryCount() + ")", true);
                continue;
            }
            if (rabbitTemplate != null && properties.isAsyncEnabled()) {
                if (outboxService.publishToBroker(row.getId())) {
                    replayed++;
                }
            } else {
                dispatcher.dispatch(row.getNotificationId(), row.getId());
                replayed++;
            }
        }
        if (replayed > 0) {
            log.info("[NOTIF-REPLAY] {} notification(s) soumise(s) au rejeu.", replayed);
        }
    }
}
