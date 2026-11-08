package com.sodepa.erp.achats.infrastructure.repo;

import com.sodepa.erp.achats.infrastructure.entities.MarcheDossierEntity;
import com.sodepa.erp.achats.infrastructure.entities.StatutMarcheDossier;
import com.sodepa.erp.achats.infrastructure.entities.TypeProcedureAchat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `MarcheDossierRepository`.
 */
@Repository
public interface MarcheDossierRepository extends JpaRepository<MarcheDossierEntity, UUID> {
    Optional<MarcheDossierEntity> findByNumeroDossier(String numeroDossier);
    Page<MarcheDossierEntity> findByTypeProcedure(TypeProcedureAchat type, Pageable pageable);
    Page<MarcheDossierEntity> findByStatut(StatutMarcheDossier statut, Pageable pageable);

    @Query("SELECT d FROM MarcheDossierEntity d WHERE d.expressionBesoin IS NULL OR d.expressionBesoin.uniteOrganisationnelle.materializedPath LIKE CONCAT(:pathPrefix, '%') ORDER BY d.dateCreation DESC")
    List<MarcheDossierEntity> findByExpressionBesoinMaterializedPathStartingWith(@Param("pathPrefix") String pathPrefix);
}
