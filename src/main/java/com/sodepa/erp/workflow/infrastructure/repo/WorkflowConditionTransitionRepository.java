package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowConditionTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowConditionTransitionRepository extends JpaRepository<WorkflowConditionTransitionEntity, UUID> {

    List<WorkflowConditionTransitionEntity> findByEtapeSourceIdOrderByPrioriteAsc(UUID etapeSourceId);
}
