package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetSourceFinancementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour budget source financement (BUDGET).
 */
@Repository
public interface BudgetSourceFinancementRepository extends JpaRepository<BudgetSourceFinancementEntity, UUID> {
    Optional<BudgetSourceFinancementEntity> findByCodeIgnoreCase(String code);
    List<BudgetSourceFinancementEntity> findByActifTrueOrderByCodeAsc();
}
