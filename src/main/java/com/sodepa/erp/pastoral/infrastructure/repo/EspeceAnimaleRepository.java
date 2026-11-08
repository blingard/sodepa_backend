package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.EspeceAnimaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour espece animale (PASTORAL).
 */
@Repository
public interface EspeceAnimaleRepository extends JpaRepository<EspeceAnimaleEntity, UUID> {
    List<EspeceAnimaleEntity> findByActifTrueOrderByCodeAsc();
    Optional<EspeceAnimaleEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
