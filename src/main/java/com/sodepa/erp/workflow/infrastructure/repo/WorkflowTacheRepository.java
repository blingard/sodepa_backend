package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowTache;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTacheEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTacheRepository extends JpaRepository<WorkflowTacheEntity, UUID> {

    List<WorkflowTacheEntity> findByWorkflowInstanceId(UUID workflowInstanceId);

    List<WorkflowTacheEntity> findByWorkflowInstanceIdAndStatut(UUID workflowInstanceId, StatutWorkflowTache statut);

    Page<WorkflowTacheEntity> findByAssigneAIdAndStatut(UUID assigneAId, StatutWorkflowTache statut, Pageable pageable);

    long countByAssigneAIdAndStatut(UUID assigneAId, StatutWorkflowTache statut);

    @Query("SELECT t FROM WorkflowTacheEntity t WHERE t.statut = 'EN_ATTENTE' AND t.dateEcheance IS NOT NULL AND t.dateEcheance < :maintenant")
    List<WorkflowTacheEntity> findTachesEnDepassementSla(@Param("maintenant") Instant maintenant);
}
