package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetDemandeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux propositions budgétaires collaboratives.
 */
@Repository
public interface BudgetDemandeRepository extends JpaRepository<BudgetDemandeEntity, UUID> {

    /**
     * Recherche toutes les propositions budgétaires d'un département pour une année spécifique.
     * 
     * @param departementId l'identifiant du département
     * @param annee l'exercice budgétaire
     * @return la liste des propositions budgétaires associées
     */
    List<BudgetDemandeEntity> findByDepartementIdAndAnnee(UUID departementId, Integer annee);

    /**
     * Recherche toutes les propositions d'une année spécifique.
     * 
     * @param annee l'exercice budgétaire
     * @return la liste des propositions
     */
    List<BudgetDemandeEntity> findByAnnee(Integer annee);

    /**
     * Demandes filtrées au besoin sur le département, l'exercice et l'état.
     *
     * <p>
     * C'est la requête de l'écran d'arbitrage : « que dois-je trancher ? ».
     * Les trois critères sont facultatifs et se combinent.
     * </p>
     */
    @Query("""
            SELECT d FROM BudgetDemandeEntity d
            WHERE (:departementId IS NULL OR d.departementId = :departementId)
              AND (:annee IS NULL OR d.annee = :annee)
              AND (:statut IS NULL OR d.statut = :statut)
            ORDER BY d.annee DESC, d.compteCode ASC
            """)
    List<BudgetDemandeEntity> rechercher(
            @Param("departementId") UUID departementId,
            @Param("annee") Integer annee,
            @Param("statut") String statut);
}
