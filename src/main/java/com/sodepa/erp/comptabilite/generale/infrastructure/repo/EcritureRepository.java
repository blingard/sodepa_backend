package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.StatutEcriture;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `EcritureRepository`.
 */
@Repository
public interface EcritureRepository extends JpaRepository<EcritureEntity, UUID> {
    List<EcritureEntity> findByDateComptableBetween(LocalDate startDate, LocalDate endDate);

    List<EcritureEntity> findByOrderByDateComptableDesc(Pageable pageable);

    @Query("SELECT e FROM EcritureEntity e JOIN FETCH e.lignes WHERE e.dateComptable BETWEEN :start AND :end")
    List<EcritureEntity> findByDateComptableBetweenWithLignes(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT DISTINCT e FROM EcritureEntity e " +
           "JOIN FETCH e.journal j " +
           "LEFT JOIN FETCH e.lignes l " +
           "LEFT JOIN FETCH l.tiers " +
           "WHERE e.statut = :statut AND e.dateComptable BETWEEN :start AND :end " +
           "ORDER BY e.dateComptable ASC, e.numeroPiece ASC")
    List<EcritureEntity> findByStatutAndDateComptableBetweenWithDetails(
            @Param("statut") StatutEcriture statut,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT DISTINCT e FROM EcritureEntity e " +
           "LEFT JOIN FETCH e.lignes l " +
           "LEFT JOIN FETCH l.ventilations v " +
           "LEFT JOIN FETCH v.section " +
           "WHERE e.statut = :statut AND e.dateComptable BETWEEN :start AND :end " +
           "ORDER BY e.dateComptable ASC")
    List<EcritureEntity> findByStatutAndDateComptableBetweenWithVentilations(
            @Param("statut") StatutEcriture statut,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT DISTINCT e FROM EcritureEntity e " +
           "LEFT JOIN FETCH e.lignes " +
           "WHERE e.valide = true AND e.dateComptable BETWEEN :start AND :end " +
           "AND e.typeDevise IS NOT NULL AND e.typeDevise NOT IN :excluded")
    List<EcritureEntity> findValidatedForeignCurrencyByPeriod(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("excluded") Collection<Devise> excluded);
}
