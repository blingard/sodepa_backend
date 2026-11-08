package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.LocaliteGeographiqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour localite geographique (PASTORAL).
 */
@Repository
public interface LocaliteGeographiqueRepository extends JpaRepository<LocaliteGeographiqueEntity, UUID> {
    List<LocaliteGeographiqueEntity> findByActifTrueOrderByCodeAsc();
    Optional<LocaliteGeographiqueEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
