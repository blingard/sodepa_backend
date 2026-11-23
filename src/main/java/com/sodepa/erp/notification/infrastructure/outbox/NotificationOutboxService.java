package com.sodepa.erp.notification.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.configuration.RabbitMqConfig;
import com.sodepa.erp.notification.infrastructure.config.NotificationProperties;
import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.NotificationOutboxEntity;
import com.sodepa.erp.notification.infrastructure.entities.OutboxStatus;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import com.sodepa.erp.notification.infrastructure.metrics.NotificationMetrics;
import com.sodepa.erp.notification.infrastructure.repo.NotificationOutboxRepository;
import com.sodepa.erp.notification.infrastructure.repo.NotificationRepository;
import com.sodepa.erp.notification.infrastructure.util.LogMasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Service de gestion transactionnelle de l'Outbox PostgreSQL et de routage RabbitMQ/DLQ des notifications.
 */
@Slf4j
@Service
public class NotificationOutboxService {

    private static final long MAX_BACKOFF_SECONDS = 3600L;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotificationOutboxService(
            NotificationOutboxRepository outboxRepository,
            NotificationRepository notificationRepository,
            NotificationProperties properties,
            NotificationMetrics metrics,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.notificationRepository = notificationRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Persiste une entrée Outbox dans la transaction en cours et programme sa publication AFTER_COMMIT.
     */
    @Transactional
    public NotificationOutboxEntity enqueue(NotificationEntity notification) {
        if (notification == null || notification.getId() == null) {
            return null;
        }
        NotificationOutboxEntity existing = outboxRepository.findByNotificationId(notification.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        String payload = serializeSafe(notification);
        NotificationOutboxEntity saved = outboxRepository.save(NotificationOutboxEntity.builder()
                .notificationId(notification.getId())
                .groupId(notification.getGroupId())
                .canal(notification.getCanal())
                .typeEvenement(notification.getTypeEvenement())
                .payloadJson(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new NotificationOutboxCreatedEvent(
                    notification.getId(),
                    saved.getId(),
                    notification.getCanal(),
                    notification.getTypeEvenement()
            ));
        }
        return saved;
    }

    /**
     * Publie une entrée Outbox vers l'échangeur RabbitMQ dédié sans conserver de verrou BDD sur l'appel réseau.
     */
    public boolean publishToBroker(UUID outboxId) {
        if (outboxId == null || rabbitTemplate == null) {
            return false;
        }
        NotificationOutboxEntity row = outboxRepository.findById(outboxId).orElse(null);
        if (row == null || row.getStatus() == OutboxStatus.COMPLETED || row.getStatus() == OutboxStatus.DEAD_LETTER) {
            return false;
        }
        String routingKey = "notif.dispatch." + row.getCanal().name().toLowerCase(Locale.ROOT);
        NotificationDispatchMessage message = new NotificationDispatchMessage(
                row.getNotificationId(),
                row.getId(),
                row.getCanal().name(),
                row.getTypeEvenement()
        );
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIF_EXCHANGE, routingKey, message);
            markPublishedSafely(outboxId);
            return true;
        } catch (Exception e) {
            String safeErr = LogMasker.sanitizeError(e.getMessage());
            log.warn("[NOTIF-OUTBOX] Échec publication RabbitMQ outboxId={}, canal={}, err={}",
                    outboxId, row.getCanal(), safeErr);
            recordFailure(row.getId(), row.getNotificationId(), safeErr, false);
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishedSafely(UUID outboxId) {
        outboxRepository.findById(outboxId).ifPresent(row -> {
            // Ne jamais écraser COMPLETED/DEAD_LETTER si le consommateur a déjà traité le message en parallèle.
            if (row.getStatus() == OutboxStatus.PENDING || row.getStatus() == OutboxStatus.FAILED) {
                row.setStatus(OutboxStatus.PUBLISHED);
                row.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(row);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID outboxId) {
        if (outboxId == null) {
            return;
        }
        outboxRepository.findById(outboxId).ifPresent(row -> {
            row.setStatus(OutboxStatus.COMPLETED);
            row.setProcessedAt(LocalDateTime.now());
            row.setLastError(null);
            outboxRepository.save(row);
        });
    }

    /**
     * Enregistre un échec d'expédition avec backoff exponentiel borné ou bascule définitive en Dead-Letter Queue.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID outboxId, UUID notificationId, String rawError, boolean publishToDlqIfExhausted) {
        String safeError = LogMasker.sanitizeError(rawError);
        int maxRetries = Math.max(1, properties.getMaxRetries());

        NotificationOutboxEntity outbox = outboxId != null ? outboxRepository.findById(outboxId).orElse(null) : null;
        if (outbox == null && notificationId != null) {
            outbox = outboxRepository.findByNotificationId(notificationId).orElse(null);
        }

        int nextAttemptCount = outbox != null ? outbox.getRetryCount() + 1 : 1;
        boolean exhausted = nextAttemptCount >= maxRetries;
        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(computeBackoffSeconds(nextAttemptCount));

        if (outbox != null) {
            outbox.setRetryCount(nextAttemptCount);
            outbox.setLastError(safeError);
            if (exhausted) {
                outbox.setStatus(OutboxStatus.DEAD_LETTER);
                outbox.setProcessedAt(LocalDateTime.now());
            } else {
                outbox.setStatus(OutboxStatus.FAILED);
                outbox.setNextRetryAt(nextRetry);
            }
            outboxRepository.save(outbox);
        }

        if (notificationId != null) {
            notificationRepository.findById(notificationId).ifPresent(notif -> {
                notif.setRetryCount(nextAttemptCount);
                notif.setErreurTechnique(safeError);
                notif.setStatut(StatutNotification.FAILED);
                notif.setNextRetryAt(exhausted ? null : nextRetry);
                notificationRepository.save(notif);
            });
        }

        if (exhausted && publishToDlqIfExhausted && rabbitTemplate != null && outbox != null) {
            try {
                NotificationDispatchMessage dlqMsg = new NotificationDispatchMessage(
                        outbox.getNotificationId(),
                        outbox.getId(),
                        outbox.getCanal().name(),
                        outbox.getTypeEvenement()
                );
                rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIF_DLX_EXCHANGE, RabbitMqConfig.ROUTING_NOTIF_DLQ, dlqMsg);
                log.error("[NOTIF-DLQ] Notification déplacée en Dead-Letter Queue après {} tentatives (notifId={}, canal={}).",
                        nextAttemptCount, notificationId, outbox.getCanal());
            } catch (Exception dlqEx) {
                log.error("[NOTIF-DLQ] Impossible d'acheminer vers DLQ RabbitMQ (notifId={}) : {}",
                        notificationId, LogMasker.sanitizeError(dlqEx.getMessage()));
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int requeueStalePublished(LocalDateTime staleBefore, int batchSize) {
        var staleRows = outboxRepository.findByStatusAndPublishedAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.PUBLISHED,
                staleBefore,
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, batchSize))
        );
        int count = 0;
        for (NotificationOutboxEntity row : staleRows) {
            row.setStatus(OutboxStatus.PENDING);
            row.setNextRetryAt(LocalDateTime.now());
            row.setPublishedAt(null);
            outboxRepository.save(row);
            count++;
        }
        return count;
    }

    public long computeBackoffSeconds(int attemptNumber) {
        long base = Math.max(1L, properties.getInitialBackoffSeconds());
        long factor = 1L << Math.min(10, Math.max(0, attemptNumber - 1));
        return Math.min(MAX_BACKOFF_SECONDS, base * factor);
    }

    private String serializeSafe(NotificationEntity notification) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "notificationId", notification.getId().toString(),
                    "canal", notification.getCanal().name(),
                    "typeEvenement", notification.getTypeEvenement(),
                    "groupId", notification.getGroupId() != null ? notification.getGroupId().toString() : "",
                    "templateCode", notification.getTemplateCode() != null ? notification.getTemplateCode() : ""
            ));
        } catch (Exception e) {
            return "{\"notificationId\":\"" + notification.getId() + "\"}";
        }
    }
}
