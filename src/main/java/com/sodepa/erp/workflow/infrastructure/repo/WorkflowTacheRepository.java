package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowTache;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour le requêtage et la manipulation des tâches de workflow assignées aux utilisateurs.
 */
@Repository
public interface WorkflowTacheRepository extends JpaRepository<WorkflowTaskEntity, UUID> {

    /**
     * Recherche l'ensemble des tâches rattachées à une instance de workflow spécifique.
     *
     * @param workflowInstanceId Identifiant de l'instance d'exécution.
     * @return Liste de toutes les tâches (historiques et courantes) de l'instance.
     */
    List<WorkflowTaskEntity> findByWorkflowInstanceId(UUID workflowInstanceId);

    /**
     * Recherche les tâches d'une instance filtrées par leur statut d'avancement (ex: EN_ATTENTE).
     *
     * @param workflowInstanceId Identifiant de l'instance d'exécution.
     * @param statut             Le statut recherché pour les tâches.
     * @return Liste des tâches correspondantes.
     */
    List<WorkflowTaskEntity> findByWorkflowInstanceIdAndStatut(UUID workflowInstanceId, StatutWorkflowTache statut);

    /**
     * Recherche toutes les tâches associées à une étape précise d'une instance de workflow.
     * Utilisé pour évaluer la complétion parallèle des tâches de l'étape.
     *
     * @param workflowInstanceId Identifiant de l'instance.
     * @param etapeDefinitionId  Identifiant de l'étape concernée.
     * @return Liste des tâches de l'étape.
     */
    List<WorkflowTaskEntity> findByWorkflowInstanceIdAndEtapeDefinitionId(UUID workflowInstanceId, UUID etapeDefinitionId);

    /**
     * Recherche avec pagination les tâches assignées à un utilisateur donné filtrées par statut
     * (alimente l'écran "Mes Tâches de validation" / corbeille de tâches de l'agent).
     *
     * @param assigneAId Identifiant de l'agent connecté.
     * @param statut     Statut des tâches recherchées (par défaut EN_ATTENTE).
     * @param pageable   Options de pagination et de tri.
     * @return Une page de tâches pour l'utilisateur.
     */
    Page<WorkflowTaskEntity> findByAssigneAIdAndStatut(UUID assigneAId, StatutWorkflowTache statut, Pageable pageable);

    /**
     * Dénombre le total de tâches ayant un certain statut pour un utilisateur donné
     * (utilisé pour les badges de notification dans la barre de navigation de l'ERP).
     *
     * @param assigneAId Identifiant de l'agent concerné.
     * @param statut     Statut ciblé (ex: EN_ATTENTE).
     * @return Nombre total de tâches correspondant au filtre.
     */
    long countByAssigneAIdAndStatut(UUID assigneAId, StatutWorkflowTache statut);

    /**
     * Recherche les tâches dont l'échéance SLA est dépassée et qui sont toujours en attente d'approbation.
     * Méthode exploitée par les batchs planifiés de surveillance SLA pour envoyer des alertes ou escalader.
     *
     * @param maintenant Horodatage de référence pour vérifier le dépassement d'échéance.
     * @return Liste des tâches en souffrance / hors délai.
     */
    @Query("SELECT t FROM WorkflowTaskEntity t WHERE t.statut = 'EN_ATTENTE' AND t.dateEcheance IS NOT NULL AND t.dateEcheance < :maintenant")
    List<WorkflowTaskEntity> findTachesEnDepassementSla(@Param("maintenant") Instant maintenant);
}
