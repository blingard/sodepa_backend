package com.sodepa.erp.tresorerie.regies.infrastructure.repo;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.MoyenPaiementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour moyen paiement (TRESORERIE).
 */
@Repository
public interface MoyenPaiementRepository extends JpaRepository<MoyenPaiementEntity, UUID> {
    Optional<MoyenPaiementEntity> findByCodeIgnoreCase(String code);
    List<MoyenPaiementEntity> findByActifTrueOrderByCodeAsc();
}
