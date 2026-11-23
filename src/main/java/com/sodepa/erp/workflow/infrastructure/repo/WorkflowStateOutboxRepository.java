package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowStateOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour la table Outbox ClickHouse du moteur de workflow.
 */
@Repository
public interface WorkflowStateOutboxRepository extends JpaRepository<WorkflowStateOutboxEntity, UUID> {

    List<WorkflowStateOutboxEntity> findBySyncedFalseOrderByCreatedAtAscVersionAsc();

    List<WorkflowStateOutboxEntity> findBySyncedFalseAndRetryCountLessThanOrderByCreatedAtAscVersionAsc(int maxRetryCount);

    List<WorkflowStateOutboxEntity> findByInstanceIdAndSyncedFalseOrderByVersionAscCreatedAtAsc(UUID instanceId);

    boolean existsByInstanceIdAndSyncedFalse(UUID instanceId);

    long countBySyncedFalse();

    Optional<WorkflowStateOutboxEntity> findTopByInstanceIdAndSyncedFalseOrderByVersionDescCreatedAtDesc(UUID instanceId);

    Optional<WorkflowStateOutboxEntity> findTopByInstanceIdOrderByVersionDescCreatedAtDesc(UUID instanceId);

    List<WorkflowStateOutboxEntity> findByInstanceIdOrderByVersionAscCreatedAtAsc(UUID instanceId);
}
