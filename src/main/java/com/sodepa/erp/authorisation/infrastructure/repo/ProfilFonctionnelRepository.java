package com.sodepa.erp.authorisation.infrastructure.repo;

import com.sodepa.erp.authorisation.infrastructure.entities.ProfilFonctionnelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour profil fonctionnel (AUTHORISATION).
 */
@Repository
public interface ProfilFonctionnelRepository extends JpaRepository<ProfilFonctionnelEntity, UUID> {
    Optional<ProfilFonctionnelEntity> findByCodeIgnoreCase(String code);
    List<ProfilFonctionnelEntity> findByActifTrueOrderByCodeAsc();
}
