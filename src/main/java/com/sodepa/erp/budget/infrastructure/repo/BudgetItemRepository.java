package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des lignes budgétaires.
 */
@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItemEntity, UUID> {

    /**
     * Recherche une ligne budgétaire par plan, compte général et section analytique.
     * 
     * @param budgetPlanId l'identifiant du plan budgétaire
     * @param compteCode le code du compte général
     * @param sectionId l'identifiant de la section analytique
     * @return un Optional contenant la ligne budgétaire
     */
    Optional<BudgetItemEntity> findByBudgetPlanIdAndCompteCodeAndSectionId(UUID budgetPlanId, String compteCode, UUID sectionId);

    /**
     * Recherche les lignes budgétaires pour un compte général et une section analytique.
     * 
     * @param compteCode le code du compte général
     * @param sectionId l'identifiant de la section analytique
     * @return la liste des lignes budgétaires correspondantes
     */
    List<BudgetItemEntity> findByCompteCodeAndSectionId(String compteCode, UUID sectionId);

    /**
     * Nombre de postes par plan, pour toute une page de plans.
     *
     * <p>
     * Une requête pour la page entière, là où lire {@code plan.getItems().size()}
     * plan par plan en aurait déclenché une par ligne affichée.
     * </p>
     */
    @Query("""
            SELECT i.budgetPlan.id AS planId, COUNT(i) AS nombre
            FROM BudgetItemEntity i
            WHERE i.budgetPlan.id IN :planIds
            GROUP BY i.budgetPlan.id
            """)
    List<DecompteItems> compterParPlan(@Param("planIds") Collection<UUID> planIds);

    /** Projection du décompte ci-dessus. */
    interface DecompteItems {
        UUID getPlanId();
        long getNombre();
    }
}
