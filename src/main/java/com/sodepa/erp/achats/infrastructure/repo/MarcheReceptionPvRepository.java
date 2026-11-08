package com.sodepa.erp.achats.infrastructure.repo;

import com.sodepa.erp.achats.infrastructure.entities.MarcheReceptionPvEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `MarcheReceptionPvRepository`.
 */
@Repository
public interface MarcheReceptionPvRepository extends JpaRepository<MarcheReceptionPvEntity, UUID> {
    Optional<MarcheReceptionPvEntity> findByNumeroPv(String numeroPv);
    List<MarcheReceptionPvEntity> findByDossierId(UUID dossierId);

    @Query("SELECT p FROM MarcheReceptionPvEntity p WHERE p.dossier.expressionBesoin IS NULL OR p.dossier.expressionBesoin.uniteOrganisationnelle.materializedPath LIKE CONCAT(:pathPrefix, '%') ORDER BY p.dateReception DESC")
    List<MarcheReceptionPvEntity> findByMaterializedPathStartingWith(@Param("pathPrefix") String pathPrefix);
}
