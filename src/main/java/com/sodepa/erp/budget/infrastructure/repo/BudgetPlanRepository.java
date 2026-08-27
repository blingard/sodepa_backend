package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetPlanEntity;
import com.sodepa.erp.budget.infrastructure.entities.StatutBudget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des plans budgétaires annuels.
 */
@Repository
public interface BudgetPlanRepository extends JpaRepository<BudgetPlanEntity, UUID> {

    /**
     * Recherche les versions d'un plan budgétaire pour un exercice donné.
     * 
     * @param annee l'exercice concerné
     * @return la liste des plans budgétaires de cette année
     */
    List<BudgetPlanEntity> findByAnnee(int annee);

    /**
     * Recherche la version spécifique d'un budget pour un exercice donné.
     * 
     * @param annee l'exercice concerné
     * @param version le numéro de version
     * @return un Optional contenant le plan budgétaire correspondant
     */
    Optional<BudgetPlanEntity> findByAnneeAndVersion(int annee, int version);

    /**
     * Plans budgétaires, filtrés au besoin sur l'exercice et l'état.
     *
     * <p>
     * Les deux critères sont facultatifs : un paramètre {@code null} ne
     * restreint rien.
     * </p>
     *
     * @param annee l'exercice, ou {@code null} pour tous
     * @param statut l'état du plan, ou {@code null} pour tous
     */
    @Query("""
            SELECT p FROM BudgetPlanEntity p
            WHERE (:annee IS NULL OR p.annee = :annee)
              AND (:statut IS NULL OR p.statut = :statut)
            """)
    Page<BudgetPlanEntity> rechercher(
            @Param("annee") Integer annee,
            @Param("statut") StatutBudget statut,
            Pageable pageable);

    /**
     * Plan et ses postes en une seule requête.
     *
     * <p>
     * La jointure évite le N+1 de la fiche : sans elle, chaque poste déclenchait
     * sa propre requête au moment du mappage.
     * </p>
     */
    @Query("SELECT p FROM BudgetPlanEntity p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Optional<BudgetPlanEntity> findByIdAvecItems(@Param("id") UUID id);
}
