package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetEngagementEntity;
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
 * Interface de persistance pour l'accès aux données des engagements budgétaires.
 */
@Repository
public interface BudgetEngagementRepository extends JpaRepository<BudgetEngagementEntity, UUID> {

    /**
     * Recherche un engagement par son numéro unique.
     * 
     * @param numeroEngagement le numéro de bon de commande ou d'engagement
     * @return un Optional contenant l'engagement correspondant
     */
    Optional<BudgetEngagementEntity> findByNumeroEngagement(String numeroEngagement);

    /**
     * Recherche les engagements associés à une ligne budgétaire.
     * 
     * @param budgetItemId l'identifiant de la ligne budgétaire
     * @return la liste des engagements associés
     */
    List<BudgetEngagementEntity> findByBudgetItemId(UUID budgetItemId);

    /**
     * Engagements d'un plan, filtrés au besoin sur leur état.
     *
     * <p>
     * L'engagement ne connaît pas le plan directement : il passe par le poste
     * budgétaire, d'où la double jointure. Les deux critères sont facultatifs.
     * </p>
     *
     * @param planId le plan visé, ou {@code null} pour tous
     * @param statut ENGAGED, CONVERTED_TO_REAL ou CANCELLED, ou {@code null} pour tous
     */
    @Query("""
            SELECT e FROM BudgetEngagementEntity e
            JOIN e.budgetItem i
            JOIN i.budgetPlan p
            WHERE (:planId IS NULL OR p.id = :planId)
              AND (:statut IS NULL OR e.statut = :statut)
            """)
    Page<BudgetEngagementEntity> rechercher(
            @Param("planId") UUID planId,
            @Param("statut") String statut,
            Pageable pageable);
}
