package com.sodepa.erp.authorisation.infrastructure.repo;

import com.sodepa.erp.authorisation.infrastructure.entities.AffectationProfilUtilisateurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour affectation profil utilisateur (AUTHORISATION).
 */
@Repository
public interface AffectationProfilUtilisateurRepository extends JpaRepository<AffectationProfilUtilisateurEntity, UUID> {
    List<AffectationProfilUtilisateurEntity> findByUtilisateurIdAndActifTrue(UUID utilisateurId);
    List<AffectationProfilUtilisateurEntity> findByProfilIdAndActifTrue(UUID profilId);
}
