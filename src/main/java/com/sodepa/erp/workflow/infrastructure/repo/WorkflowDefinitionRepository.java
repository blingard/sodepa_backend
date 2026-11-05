package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDefinitionEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, UUID> {

    Optional<WorkflowDefinitionEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<WorkflowDefinitionEntity> findByDomaineAndActifTrue(WorkflowDomaine domaine);

    List<WorkflowDefinitionEntity> findAllByActifTrue();
}
