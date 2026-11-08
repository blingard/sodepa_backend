package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.DerogationPermissionEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDerogation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `DerogationPermissionRepository`.
 */
@Repository
public interface DerogationPermissionRepository extends JpaRepository<DerogationPermissionEntity, UUID> {
    List<DerogationPermissionEntity> findByUtilisateurIdAndStatut(UUID utilisateurId, StatutDerogation statut);
    List<DerogationPermissionEntity> findByUtilisateurId(UUID utilisateurId);
}
