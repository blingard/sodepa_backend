package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowEtapeActeurDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour la gestion des acteurs configurés sur les étapes de workflow.
 */
@Repository
public interface WorkflowEtapeActeurDefinitionRepository extends JpaRepository<WorkflowEtapeActeurDefinitionEntity, UUID> {

    /**
     * Recherche tous les acteurs configurés pour une étape de workflow donnée.
     *
     * @param etapeDefinitionId Identifiant unique de l'étape.
     * @return Liste des acteurs attendus.
     */
    List<WorkflowEtapeActeurDefinitionEntity> findByEtapeDefinitionId(UUID etapeDefinitionId);
}
