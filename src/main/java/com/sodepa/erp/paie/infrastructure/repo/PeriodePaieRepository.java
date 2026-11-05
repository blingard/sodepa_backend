package com.sodepa.erp.paie.infrastructure.repo;

import com.sodepa.erp.paie.infrastructure.entities.PeriodePaieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeriodePaieRepository extends JpaRepository<PeriodePaieEntity, UUID> {
    Optional<PeriodePaieEntity> findByCode(String code);
    Optional<PeriodePaieEntity> findByAnneeAndMois(Integer annee, Integer mois);
}
