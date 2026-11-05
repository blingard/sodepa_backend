package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowEtapeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowEtapeDefinitionRepository extends JpaRepository<WorkflowEtapeDefinitionEntity, UUID> {

    List<WorkflowEtapeDefinitionEntity> findByWorkflowDefinitionIdOrderByOrdreAsc(UUID workflowDefinitionId);

    Optional<WorkflowEtapeDefinitionEntity> findFirstByWorkflowDefinitionIdOrderByOrdreAsc(UUID workflowDefinitionId);

    Optional<WorkflowEtapeDefinitionEntity> findByWorkflowDefinitionIdAndCodeEtape(UUID workflowDefinitionId, String codeEtape);
}
