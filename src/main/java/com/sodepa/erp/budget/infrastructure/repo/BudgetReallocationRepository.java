package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetReallocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux transferts de budgets.
 */
@Repository
public interface BudgetReallocationRepository extends JpaRepository<BudgetReallocationEntity, UUID> {

    /**
     * Recherche les transferts dont la ligne source est spécifiée.
     * 
     * @param sourceItemId l'identifiant de la ligne source
     * @return la liste des transferts sortants
     */
    List<BudgetReallocationEntity> findBySourceItemId(UUID sourceItemId);

    /**
     * Recherche les transferts dont la ligne de destination est spécifiée.
     * 
     * @param destItemId l'identifiant de la ligne de destination
     * @return la liste des transferts entrants
     */
    List<BudgetReallocationEntity> findByDestItemId(UUID destItemId);
}
