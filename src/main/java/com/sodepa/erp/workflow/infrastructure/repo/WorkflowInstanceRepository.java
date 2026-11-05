package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowInstance;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {

    Optional<WorkflowInstanceEntity> findByEntityNameAndEntityId(String entityName, String entityId);

    List<WorkflowInstanceEntity> findByInitiateurId(UUID initiateurId);

    Page<WorkflowInstanceEntity> findByStatut(StatutWorkflowInstance statut, Pageable pageable);

    Page<WorkflowInstanceEntity> findByEntityName(String entityName, Pageable pageable);
}
