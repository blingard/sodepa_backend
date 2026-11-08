package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.AchatSeuilConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `AchatSeuilConfigRepository`.
 */
@Repository
public interface AchatSeuilConfigRepository extends JpaRepository<AchatSeuilConfigEntity, UUID> {
    Optional<AchatSeuilConfigEntity> findByTypeProcedure(String typeProcedure);
    List<AchatSeuilConfigEntity> findByActifTrueOrderByMontantMinTtcAsc();
}
