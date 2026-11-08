package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.PieceConformiteTiersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour piece conformite tiers (COMPTABILITE).
 */
@Repository
public interface PieceConformiteTiersRepository extends JpaRepository<PieceConformiteTiersEntity, UUID> {

    List<PieceConformiteTiersEntity> findByTiersIdOrderByDateExpirationAsc(UUID tiersId);

    /**
     * Détecte les pièces arrivant à expiration d'ici {@code dateHorizon} (ex: J+15) ou déjà expirées (CA-REF-023-01).
     */
    @Query("SELECT p FROM PieceConformiteTiersEntity p WHERE p.dateExpiration IS NOT NULL AND p.dateExpiration <= :dateHorizon ORDER BY p.dateExpiration ASC")
    List<PieceConformiteTiersEntity> findPiecesExpirantAvant(@Param("dateHorizon") LocalDate dateHorizon);
}
