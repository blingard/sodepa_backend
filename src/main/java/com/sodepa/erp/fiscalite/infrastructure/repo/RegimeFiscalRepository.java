package com.sodepa.erp.fiscalite.infrastructure.repo;

import com.sodepa.erp.fiscalite.infrastructure.entities.RegimeFiscalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour regime fiscal (FISCALITE).
 */
@Repository
public interface RegimeFiscalRepository extends JpaRepository<RegimeFiscalEntity, UUID> {
    Optional<RegimeFiscalEntity> findByCodeIgnoreCase(String code);
    List<RegimeFiscalEntity> findByActifTrueOrderByCodeAsc();
}
