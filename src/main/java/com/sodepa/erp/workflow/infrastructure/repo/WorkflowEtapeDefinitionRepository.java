package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowEtapeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour la persistance et le requêtage des étapes d'un circuit de validation.
 */
@Repository
public interface WorkflowEtapeDefinitionRepository extends JpaRepository<WorkflowEtapeDefinitionEntity, UUID> {

    /**
     * Récupère toutes les étapes appartenant à un circuit de validation donné, triées par ordre croissant.
     *
     * @param workflowDefinitionId Identifiant du circuit parent.
     * @return Liste ordonnée des étapes définies.
     */
    List<WorkflowEtapeDefinitionEntity> findByWorkflowDefinitionIdOrderByOrdreAsc(UUID workflowDefinitionId);

    /**
     * Recherche la toute première étape (ordre séquentiel minimal) d'un circuit de validation.
     * Utilisé lors de l'initiation d'une nouvelle instance pour assigner la première tâche.
     *
     * @param workflowDefinitionId Identifiant du circuit de workflow.
     * @return Un {@link Optional} contenant la première étape si elle existe.
     */
    Optional<WorkflowEtapeDefinitionEntity> findFirstByWorkflowDefinitionIdOrderByOrdreAsc(UUID workflowDefinitionId);

    /**
     * Recherche une étape spécifique par son code au sein d'une définition de circuit.
     *
     * @param workflowDefinitionId Identifiant du circuit de workflow.
     * @param codeEtape            Code unique de l'étape au sein du circuit.
     * @return Un {@link Optional} contenant l'étape trouvée.
     */
    Optional<WorkflowEtapeDefinitionEntity> findByWorkflowDefinitionIdAndCodeEtape(UUID workflowDefinitionId, String codeEtape);
}
