package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.UniteMesureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour unite mesure (PASTORAL).
 */
@Repository
public interface UniteMesureRepository extends JpaRepository<UniteMesureEntity, UUID> {
    List<UniteMesureEntity> findByActifTrueOrderByCodeAsc();
    Optional<UniteMesureEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
