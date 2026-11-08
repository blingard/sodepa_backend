package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.ObjectifUniteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutObjectif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `ObjectifUniteRepository`.
 */
@Repository
public interface ObjectifUniteRepository extends JpaRepository<ObjectifUniteEntity, UUID> {

    List<ObjectifUniteEntity> findByUnite_IdOrderByCreatedAtDesc(UUID uniteId);

    List<ObjectifUniteEntity> findByUnite_IdAndAnneeExerciceOrderByCreatedAtDesc(UUID uniteId, Integer anneeExercice);

    List<ObjectifUniteEntity> findByAnneeExerciceAndStatut(Integer anneeExercice, StatutObjectif statut);

    @Query("SELECT AVG(o.tauxAtteinte) FROM ObjectifUniteEntity o WHERE o.unite.id = :uniteId")
    BigDecimal calculateAverageTauxByUniteId(@Param("uniteId") UUID uniteId);

    @Query("SELECT COUNT(o) FROM ObjectifUniteEntity o WHERE o.unite.id = :uniteId")
    long countByUniteId(@Param("uniteId") UUID uniteId);
}
