package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowEtapeActeurDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT w.etapeDefinition FROM WorkflowEtapeActeurDefinitionEntity w WHERE w.employe = :employe AND w.utilisateur = :utilisateur AND w.uniteOrganisationnelle = :uniteOrganisationnelle")
    List<WorkflowEtapeActeurDefinitionEntity> findByEmployeAndUtilisateurAndUniteOrganisationnelle(EmployeEntity employe, UtilisateurEntity utilisateur, UniteOrganisationnelleEntity uniteOrganisationnelle);
}
