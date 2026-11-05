package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowHistoriqueAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowHistoriqueAuditRepository extends JpaRepository<WorkflowHistoriqueAuditEntity, UUID> {

    List<WorkflowHistoriqueAuditEntity> findByWorkflowInstanceIdOrderByHorodatageAsc(UUID workflowInstanceId);
}
