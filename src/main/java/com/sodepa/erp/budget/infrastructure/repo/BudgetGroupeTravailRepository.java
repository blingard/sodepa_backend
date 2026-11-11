package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetGroupeTravailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour les groupes de travail budgétaires.
 */
@Repository
public interface BudgetGroupeTravailRepository extends JpaRepository<BudgetGroupeTravailEntity, UUID> {
    List<BudgetGroupeTravailEntity> findByCampagneId(UUID campagneId);
}
