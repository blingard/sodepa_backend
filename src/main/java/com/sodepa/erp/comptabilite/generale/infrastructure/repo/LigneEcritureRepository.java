package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.utils.StatutEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Query("SELECT l.compteCode, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) " +
           "FROM LigneEcritureEntity l " +
           "WHERE l.ecriture.statut = :statut " +
           "GROUP BY l.compteCode")
    List<Object[]> sumMovementsByCompteCodeForStatut(@Param("statut") StatutEcriture statut);

    @Query("SELECT l.compteCode, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) " +
           "FROM LigneEcritureEntity l " +
           "WHERE l.ecriture.valide = true AND l.ecriture.dateComptable < :debut " +
           "GROUP BY l.compteCode")
    List<Object[]> sumValidatedMovementsBeforeDate(@Param("debut") LocalDate debut);

    @Query("SELECT l.compteCode, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) " +
           "FROM LigneEcritureEntity l " +
           "WHERE l.ecriture.valide = true AND l.ecriture.dateComptable BETWEEN :debut AND :fin " +
           "GROUP BY l.compteCode")
    List<Object[]> sumValidatedMovementsBetweenDates(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT l FROM LigneEcritureEntity l " +
           "JOIN FETCH l.ecriture e " +
           "JOIN FETCH e.journal j " +
           "LEFT JOIN FETCH l.tiers t " +
           "WHERE e.valide = true AND e.dateComptable BETWEEN :debut AND :fin " +
           "ORDER BY e.dateComptable ASC, e.numeroPiece ASC")
    List<LigneEcritureEntity> findValidatedLinesBetweenDates(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT l.compteCode, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) " +
           "FROM LigneEcritureEntity l " +
           "WHERE l.ecriture.statut = :statut AND l.ecriture.dateComptable < :before " +
           "GROUP BY l.compteCode")
    List<Object[]> sumMovementsBeforeDateForStatut(@Param("statut") StatutEcriture statut, @Param("before") LocalDate before);

    @Query("SELECT l.compteCode, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) " +
           "FROM LigneEcritureEntity l " +
           "WHERE l.ecriture.statut = :statut AND l.ecriture.dateComptable BETWEEN :debut AND :fin " +
           "GROUP BY l.compteCode")
    List<Object[]> sumMovementsBetweenDatesForStatut(
            @Param("statut") StatutEcriture statut,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.debit > 0 THEN l.debit ELSE l.credit END), 0) " +
           "FROM LigneEcritureEntity l " +
           "WHERE l.ecriture.statut = :statut AND l.compteCode LIKE CONCAT(:prefix, '%')")
    BigDecimal sumLineAmountsByComptePrefixAndStatut(@Param("prefix") String prefix, @Param("statut") StatutEcriture statut);
}
