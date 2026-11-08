package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetNatureDepenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour budget nature depense (BUDGET).
 */
@Repository
public interface BudgetNatureDepenseRepository extends JpaRepository<BudgetNatureDepenseEntity, UUID> {
    Optional<BudgetNatureDepenseEntity> findByCodeIgnoreCase(String code);
    List<BudgetNatureDepenseEntity> findByActifTrueOrderByCodeAsc();
}
