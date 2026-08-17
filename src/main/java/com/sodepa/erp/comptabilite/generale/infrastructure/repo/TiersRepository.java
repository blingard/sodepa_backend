package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux tiers (comptes auxiliaires).
 */
@Repository
public interface TiersRepository extends JpaRepository<TiersEntity, UUID> {
    Optional<TiersEntity> findByCode(String code);
    Optional<TiersEntity> findByIdAndActifIsTrue(UUID id);
    List<TiersEntity> findAllByActifIsTrueOrderByRaisonSocialeAsc();
}
