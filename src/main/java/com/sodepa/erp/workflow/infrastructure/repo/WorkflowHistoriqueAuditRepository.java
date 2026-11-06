package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowHistoriqueAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour la persistance et la consultation de la piste d'audit des workflows.
 */
@Repository
public interface WorkflowHistoriqueAuditRepository extends JpaRepository<WorkflowHistoriqueAuditEntity, UUID> {

    /**
     * Récupère l'historique chronologique complet des actions d'audit pour une instance de workflow.
     *
     * @param workflowInstanceId Identifiant unique de l'instance d'exécution.
     * @return Liste ordonnée chronologiquement de toutes les traces d'audit.
     */
    List<WorkflowHistoriqueAuditEntity> findByWorkflowInstanceIdOrderByHorodatageAsc(UUID workflowInstanceId);
}
