package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetReunionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour les réunions et concertations budgétaires.
 */
@Repository
public interface BudgetReunionRepository extends JpaRepository<BudgetReunionEntity, UUID> {
    List<BudgetReunionEntity> findByGroupeTravailIdOrderByDateReunionDesc(UUID groupeTravailId);
}
