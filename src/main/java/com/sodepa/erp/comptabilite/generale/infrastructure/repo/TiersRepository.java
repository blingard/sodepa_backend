package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.utils.TypeTiers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour la gestion du référentiel unifié des tiers et partenaires (REF-05).
 */
@Repository
public interface TiersRepository extends JpaRepository<TiersEntity, UUID> {

    Optional<TiersEntity> findByCode(String code);

    Optional<TiersEntity> findByIdAndActifIsTrue(UUID id);

    List<TiersEntity> findAllByActifIsTrueOrderByRaisonSocialeAsc();

    List<TiersEntity> findByTypeTiersAndActifIsTrueOrderByRaisonSocialeAsc(TypeTiers typeTiers);

    boolean existsByNumeroContribuableIgnoreCase(String numeroContribuable);

    boolean existsByRaisonSocialeIgnoreCase(String raisonSociale);
}
