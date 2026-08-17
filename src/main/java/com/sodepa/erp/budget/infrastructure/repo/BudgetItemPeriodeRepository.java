package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetItemPeriodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des découpages périodiques de budgets.
 */
@Repository
public interface BudgetItemPeriodeRepository extends JpaRepository<BudgetItemPeriodeEntity, UUID> {

    /**
     * Recherche les périodes d'une ligne budgétaire donnée.
     * 
     * @param budgetItemId l'identifiant de la ligne budgétaire
     * @return la liste des périodes associées
     */
    List<BudgetItemPeriodeEntity> findByBudgetItemId(UUID budgetItemId);
}
