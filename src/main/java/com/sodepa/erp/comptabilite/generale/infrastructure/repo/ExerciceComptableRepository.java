package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ExerciceComptableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour exercice comptable (COMPTABILITE).
 */
@Repository
public interface ExerciceComptableRepository extends JpaRepository<ExerciceComptableEntity, UUID> {
    Optional<ExerciceComptableEntity> findByAnnee(Integer annee);
    List<ExerciceComptableEntity> findAllByOrderByAnneeDesc();
}
