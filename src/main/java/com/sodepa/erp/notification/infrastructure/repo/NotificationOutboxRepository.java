package com.sodepa.erp.notification.infrastructure.repo;

import com.sodepa.erp.notification.infrastructure.entities.NotificationOutboxEntity;
import com.sodepa.erp.notification.infrastructure.entities.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Référentiel d'accès à la table Outbox des notifications asynchrones.
 */
@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, UUID> {

    Optional<NotificationOutboxEntity> findByNotificationId(UUID notificationId);

    List<NotificationOutboxEntity> findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            Collection<OutboxStatus> statuses,
            LocalDateTime cutoff,
            Pageable pageable);

    List<NotificationOutboxEntity> findByStatusAndPublishedAtBeforeOrderByCreatedAtAsc(
            OutboxStatus status,
            LocalDateTime staleBefore,
            Pageable pageable);

    long countByStatusIn(Collection<OutboxStatus> statuses);
}
