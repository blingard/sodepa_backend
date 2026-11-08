package com.sodepa.erp.dashboard.infrastructure.repo;

import com.sodepa.erp.dashboard.infrastructure.entities.FamilleReferentielEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour famille referentiel (DASHBOARD).
 */
@Repository
public interface FamilleReferentielRepository extends JpaRepository<FamilleReferentielEntity, UUID> {
    Optional<FamilleReferentielEntity> findByCodeIgnoreCase(String code);
    List<FamilleReferentielEntity> findByProprietaireMetierIgnoreCaseOrderByCodeAsc(String proprietaireMetier);
    boolean existsByCodeIgnoreCase(String code);
}
