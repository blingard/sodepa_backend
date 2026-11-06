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

/**
 * Interface Spring Data JPA pour la gestion des instances d'exécution de workflow.
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {

    /**
     * Recherche l'instance de workflow associée à une entité métier spécifique (liaison 1-1 active).
     *
     * @param entityName Nom fonctionnel de l'entité (ex: "ENGAGEMENT_BUDGETAIRE", "BON_COMMANDE").
     * @param entityId   Identifiant technique de l'entité métier.
     * @return Un {@link Optional} contenant l'instance de workflow associée si trouvée.
     */
    Optional<WorkflowInstanceEntity> findByEntityNameAndEntityId(String entityName, String entityId);

    /**
     * Recherche toutes les instances initiées par un collaborateur particulier.
     *
     * @param initiateurId Identifiant unique de l'utilisateur initiateur.
     * @return Liste des instances lancées par cet utilisateur.
     */
    List<WorkflowInstanceEntity> findByInitiateurId(UUID initiateurId);

    /**
     * Récupère une page d'instances filtrées par leur statut d'avancement global (EN_COURS, APPROUVE, REJETE, etc.).
     *
     * @param statut   Le statut recherché.
     * @param pageable Les options de pagination et de tri.
     * @return Une page d'instances correspondantes.
     */
    Page<WorkflowInstanceEntity> findByStatut(StatutWorkflowInstance statut, Pageable pageable);

    /**
     * Récupère une page d'instances filtrées par type d'entité métier ciblée.
     *
     * @param entityName Le type d'entité métier recherché.
     * @param pageable   Les options de pagination et de tri.
     * @return Une page d'instances correspondantes.
     */
    Page<WorkflowInstanceEntity> findByEntityName(String entityName, Pageable pageable);
}
