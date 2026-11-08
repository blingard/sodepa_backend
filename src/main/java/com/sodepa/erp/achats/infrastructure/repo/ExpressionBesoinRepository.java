package com.sodepa.erp.achats.infrastructure.repo;

import com.sodepa.erp.achats.infrastructure.entities.ExpressionBesoinEntity;
import com.sodepa.erp.achats.infrastructure.entities.StatutExpressionBesoin;
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
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `ExpressionBesoinRepository`.
 */
@Repository
public interface ExpressionBesoinRepository extends JpaRepository<ExpressionBesoinEntity, UUID> {
    Optional<ExpressionBesoinEntity> findByNumero(String numero);
    Page<ExpressionBesoinEntity> findByStatut(StatutExpressionBesoin statut, Pageable pageable);
    Page<ExpressionBesoinEntity> findByUniteOrganisationnelleId(UUID uniteId, Pageable pageable);

    @Query("SELECT e FROM ExpressionBesoinEntity e WHERE e.uniteOrganisationnelle.materializedPath LIKE CONCAT(:pathPrefix, '%') ORDER BY e.dateCreation DESC")
    List<ExpressionBesoinEntity> findByMaterializedPathStartingWith(@Param("pathPrefix") String pathPrefix);
}
