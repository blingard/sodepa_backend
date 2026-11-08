package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `LigneEcritureRepository`.
 */
@Repository
public interface LigneEcritureRepository extends JpaRepository<LigneEcritureEntity, UUID> {
    List<LigneEcritureEntity> findByCompteCode(String compteCode);
    List<LigneEcritureEntity> findByTiersId(UUID tiersId);
    
    // For bank reconciliation
    List<LigneEcritureEntity> findByCompteCodeAndIdNotIn(String compteCode, List<UUID> excludedIds);
}
