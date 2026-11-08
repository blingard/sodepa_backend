package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.RegleConversionUniteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour regle conversion unite (PASTORAL).
 */
@Repository
public interface RegleConversionUniteRepository extends JpaRepository<RegleConversionUniteEntity, UUID> {
    List<RegleConversionUniteEntity> findByActifTrue();
    Optional<RegleConversionUniteEntity> findByCodeUniteSourceIgnoreCaseAndCodeUniteCibleIgnoreCaseAndActifTrue(
            String codeUniteSource, String codeUniteCible);
}
