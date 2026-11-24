package com.sodepa.erp.budgetaire.exercice.infrastructure.repo;

import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.ExerciceBudgetaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Accès aux exercices budgétaires. */
public interface ExerciceBudgetaireRepository extends JpaRepository<ExerciceBudgetaireEntity, UUID> {

    boolean existsByAnnee(Integer annee);

    Optional<ExerciceBudgetaireEntity> findByAnnee(Integer annee);
}
