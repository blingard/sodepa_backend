package com.sodepa.erp.fiscalite.infrastructure.repo;

import com.sodepa.erp.fiscalite.infrastructure.entities.SeuilGestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour seuil gestion (FISCALITE).
 */
@Repository
public interface SeuilGestionRepository extends JpaRepository<SeuilGestionEntity, UUID> {

    List<SeuilGestionEntity> findByActifTrueOrderByDomaineAscMontantMinFcfaAsc();

    List<SeuilGestionEntity> findByDomaineIgnoreCaseAndActifTrueOrderByMontantMinFcfaAsc(String domaine);

    List<SeuilGestionEntity> findByCodeIgnoreCaseOrderByDateEffetDesc(String code);

    Optional<SeuilGestionEntity> findFirstByCodeIgnoreCaseAndActifTrueAndDateFinIsNullOrderByDateEffetDesc(String code);

    /**
     * Résout le seuil applicable pour un code donné à une date précise (CA-REF-020-03).
     */
    @Query("SELECT s FROM SeuilGestionEntity s WHERE UPPER(s.code) = UPPER(:code) AND s.actif = true " +
           "AND s.dateEffet <= :dateOperation AND (s.dateFin IS NULL OR s.dateFin >= :dateOperation) " +
           "ORDER BY s.dateEffet DESC")
    List<SeuilGestionEntity> findSeuilApplicableParCodeEtDate(
            @Param("code") String code,
            @Param("dateOperation") LocalDate dateOperation);

    /**
     * Résout la règle de seuil d'un domaine (ex: ACHATS) correspondant à un montant TTC et une date d'opération (CA-REF-020-02 & 03).
     */
    @Query("SELECT s FROM SeuilGestionEntity s WHERE UPPER(s.domaine) = UPPER(:domaine) AND s.actif = true " +
           "AND s.dateEffet <= :dateOperation AND (s.dateFin IS NULL OR s.dateFin >= :dateOperation) " +
           "AND s.montantMinFcfa <= :montant AND (s.montantMaxFcfa IS NULL OR s.montantMaxFcfa >= :montant) " +
           "ORDER BY s.dateEffet DESC, s.montantMinFcfa DESC")
    List<SeuilGestionEntity> findSeuilPourDomaineMontantEtDate(
            @Param("domaine") String domaine,
            @Param("montant") BigDecimal montant,
            @Param("dateOperation") LocalDate dateOperation);
}
