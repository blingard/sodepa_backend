package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowConditionTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour l'accès aux données des conditions de transition d'étapes.
 * <p>
 * Permet d'extraire les règles de routage dynamique associées à une étape source, ordonnées par priorité.
 * </p>
 */
@Repository
public interface WorkflowConditionTransitionRepository extends JpaRepository<WorkflowConditionTransitionEntity, UUID> {

    /**
     * Recherche toutes les transitions conditionnelles sortantes d'une étape source donnée,
     * triées par ordre de priorité croissante (la plus prioritaire en premier).
     *
     * @param etapeSourceId Identifiant unique de l'étape source.
     * @return Liste ordonnée des conditions de transition configurées.
     */
    List<WorkflowConditionTransitionEntity> findByEtapeSourceIdOrderByPrioriteAsc(UUID etapeSourceId);
}
