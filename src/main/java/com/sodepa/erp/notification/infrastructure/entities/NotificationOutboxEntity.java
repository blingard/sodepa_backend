package com.sodepa.erp.notification.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité Outbox PostgreSQL garantissant la durabilité transactionnelle des expéditions asynchrones de notifications.
 */
@Entity
@Table(name = "sys_notification_outbox", indexes = {
        @Index(name = "idx_notif_outbox_pending", columnList = "status, next_retry_at, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutboxEntity {

    @Id
    private UUID id;

    @PrePersist
    void initDefaults() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.nextRetryAt == null) {
            this.nextRetryAt = LocalDateTime.now();
        }
    }

    @Column(name = "notification_id", nullable = false, unique = true)
    private UUID notificationId;

    @Column(name = "group_id")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CanalNotification canal;

    @Column(name = "type_evenement", nullable = false, length = 60)
    private String typeEvenement;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "next_retry_at", nullable = false)
    @Builder.Default
    private LocalDateTime nextRetryAt = LocalDateTime.now();

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
