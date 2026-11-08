package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetActiviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour budget activite (BUDGET).
 */
@Repository
public interface BudgetActiviteRepository extends JpaRepository<BudgetActiviteEntity, UUID> {
    Optional<BudgetActiviteEntity> findByCodeIgnoreCase(String code);
    List<BudgetActiviteEntity> findByActifTrueOrderByCodeAsc();
    List<BudgetActiviteEntity> findByProgrammeCodeIgnoreCaseAndActifTrue(String programmeCode);
}
