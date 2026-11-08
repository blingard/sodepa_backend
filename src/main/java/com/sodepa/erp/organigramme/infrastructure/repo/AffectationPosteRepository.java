package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeAffectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `AffectationPosteRepository`.
 */
@Repository
public interface AffectationPosteRepository extends JpaRepository<AffectationPosteEntity, UUID> {

    List<AffectationPosteEntity> findByUtilisateurIdAndStatut(UUID utilisateurId, StatutAffectation statut);

    Optional<AffectationPosteEntity> findFirstByUtilisateurIdAndTypeAffectationAndStatut(
            UUID utilisateurId, TypeAffectation typeAffectation, StatutAffectation statut);

    List<AffectationPosteEntity> findByPosteIdAndStatut(UUID posteId, StatutAffectation statut);

    List<AffectationPosteEntity> findByPosteId(UUID posteId);

    List<AffectationPosteEntity> findByUtilisateurId(UUID utilisateurId);

    long countByPosteIdAndStatut(UUID posteId, StatutAffectation statut);
}
