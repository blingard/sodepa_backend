package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetProgrammeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour budget programme (BUDGET).
 */
@Repository
public interface BudgetProgrammeRepository extends JpaRepository<BudgetProgrammeEntity, UUID> {
    Optional<BudgetProgrammeEntity> findByCodeIgnoreCase(String code);
    List<BudgetProgrammeEntity> findByActifTrueOrderByCodeAsc();
}
