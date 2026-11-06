package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDefinitionEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour la gestion et le requêtage des modèles de circuits de validation.
 */
@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, UUID> {

    /**
     * Recherche une définition de circuit par son code fonctionnel unique (ex: "WF-BUDGET-ENGAGEMENT").
     *
     * @param code Le code fonctionnel standardisé du circuit.
     * @return Un {@link Optional} contenant le circuit s'il existe.
     */
    Optional<WorkflowDefinitionEntity> findByCode(String code);

    /**
     * Vérifie si un circuit existe déjà en base de données pour un code donné.
     *
     * @param code Le code du circuit à vérifier.
     * @return {@code true} si un circuit possède déjà ce code, {@code false} sinon.
     */
    boolean existsByCode(String code);

    /**
     * Recherche le circuit actif configuré pour un métier et une unité organisationnelle donnés.
     *
     * @param metierId                Identifiant de l'opération métier.
     * @param uniteOrganisationnelleId Identifiant de l'unité propriétaire.
     * @return Le circuit actif correspondant.
     */
    Optional<WorkflowDefinitionEntity> findByMetierIdAndUniteOrganisationnelleIdAndActifTrue(UUID metierId, UUID uniteOrganisationnelleId);

    /**
     * Recherche le circuit actif par code métier et unité organisationnelle.
     *
     * @param metierCode              Code de l'opération métier (ex: "RECRUITMENT").
     * @param uniteOrganisationnelleId Identifiant de l'unité.
     * @return Le circuit actif correspondant.
     */
    @Query("SELECT w FROM WorkflowDefinitionEntity w WHERE w.metier.code = :metierCode AND w.uniteOrganisationnelle.id = :uniteId AND w.actif = true")
    Optional<WorkflowDefinitionEntity> findByMetierCodeAndUniteId(@Param("metierCode") String metierCode, @Param("uniteId") UUID uniteOrganisationnelleId);

    /**
     * Récupère tous les circuits rattachés à une unité organisationnelle.
     *
     * @param uniteOrganisationnelleId Identifiant de l'unité.
     * @return Liste des définitions de workflow.
     */
    List<WorkflowDefinitionEntity> findByUniteOrganisationnelleId(UUID uniteOrganisationnelleId);

    /**
     * Récupère les circuits transverses globaux (sans unité restreinte).
     *
     * @return Liste des workflows transverses.
     */
    List<WorkflowDefinitionEntity> findByUniteOrganisationnelleIsNullAndActifTrue();

    /**
     * Récupère tous les circuits de validation actifs associés à un domaine fonctionnel particulier.
     *
     * @param domaine Le domaine métier (BUDGET, RH, ACHATS, etc.).
     * @return Liste des circuits actifs dans ce domaine.
     */
    List<WorkflowDefinitionEntity> findByDomaineAndActifTrue(WorkflowDomaine domaine);

    /**
     * Récupère la totalité des circuits de validation actuellement actifs dans le système.
     *
     * @return Liste des définitions de workflow en état actif.
     */
    List<WorkflowDefinitionEntity> findAllByActifTrue();
}
