package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetCampagneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour la gestion des campagnes annuelles de préparation budgétaire.
 */
@Repository
public interface BudgetCampagneRepository extends JpaRepository<BudgetCampagneEntity, UUID> {
    Optional<BudgetCampagneEntity> findByAnnee(Integer annee);
}
