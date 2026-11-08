package com.sodepa.erp.dashboard.infrastructure.repo;

import com.sodepa.erp.dashboard.infrastructure.entities.FicheReferentielVersionEntity;
import com.sodepa.erp.utils.StatutCycleVieReferentiel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour fiche referentiel version (DASHBOARD).
 */
@Repository
public interface FicheReferentielVersionRepository extends JpaRepository<FicheReferentielVersionEntity, UUID> {
    List<FicheReferentielVersionEntity> findByCodeFamilleIgnoreCaseAndCodeFicheIgnoreCaseOrderByNumeroVersionDesc(
            String codeFamille, String codeFiche);

    Optional<FicheReferentielVersionEntity> findFirstByCodeFamilleIgnoreCaseAndCodeFicheIgnoreCaseOrderByNumeroVersionDesc(
            String codeFamille, String codeFiche);

    List<FicheReferentielVersionEntity> findByCodeFamilleIgnoreCaseAndStatutCycleVieOrderByCodeFicheAsc(
            String codeFamille, StatutCycleVieReferentiel statutCycleVie);

    List<FicheReferentielVersionEntity> findByCodeFamilleIgnoreCaseOrderByCodeFicheAscNumeroVersionDesc(
            String codeFamille);
}
