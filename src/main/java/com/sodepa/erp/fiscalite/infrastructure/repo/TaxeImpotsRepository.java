package com.sodepa.erp.fiscalite.infrastructure.repo;

import com.sodepa.erp.fiscalite.infrastructure.entities.TaxeImpotsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour taxe impots (FISCALITE).
 */
@Repository
public interface TaxeImpotsRepository extends JpaRepository<TaxeImpotsEntity, UUID> {

    List<TaxeImpotsEntity> findByActifTrueOrderByCodeAscDateEffetDesc();

    List<TaxeImpotsEntity> findByCodeIgnoreCaseOrderByDateEffetDesc(String code);

    /**
     * Résout la taxe applicable à une date d'opération donnée (CA-REF-020-01 / RG-REF-007).
     */
    @Query("SELECT t FROM TaxeImpotsEntity t WHERE UPPER(t.code) = UPPER(:code) AND t.actif = true " +
           "AND t.dateEffet <= :dateOperation AND (t.dateFin IS NULL OR t.dateFin >= :dateOperation) " +
           "ORDER BY t.dateEffet DESC")
    List<TaxeImpotsEntity> findApplicableAtDate(@Param("code") String code, @Param("dateOperation") LocalDate dateOperation);

    Optional<TaxeImpotsEntity> findFirstByCodeIgnoreCaseAndActifTrueAndDateFinIsNullOrderByDateEffetDesc(String code);
}
