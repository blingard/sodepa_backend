package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.PeriodeComptableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour periode comptable (COMPTABILITE).
 */
@Repository
public interface PeriodeComptableRepository extends JpaRepository<PeriodeComptableEntity, UUID> {
    List<PeriodeComptableEntity> findByExerciceAnneeOrderByNumeroMoisAsc(Integer annee);
    Optional<PeriodeComptableEntity> findByCodePeriode(String codePeriode);
}
