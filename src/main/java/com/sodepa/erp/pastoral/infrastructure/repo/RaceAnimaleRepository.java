package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.RaceAnimaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour race animale (PASTORAL).
 */
@Repository
public interface RaceAnimaleRepository extends JpaRepository<RaceAnimaleEntity, UUID> {
    List<RaceAnimaleEntity> findByActifTrueOrderByCodeAsc();
    List<RaceAnimaleEntity> findByCodeEspeceIgnoreCaseAndActifTrueOrderByCodeAsc(String codeEspece);
    Optional<RaceAnimaleEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
