package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralBovinEntity;
import com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `PastoralBovinRepository`.
 */
@Repository
public interface PastoralBovinRepository extends JpaRepository<PastoralBovinEntity, UUID> {
    Optional<PastoralBovinEntity> findByMatriculeBoucle(String matriculeBoucle);
    List<PastoralBovinEntity> findByRanchIdAndStatut(UUID ranchId, StatutBovin statut);
    Page<PastoralBovinEntity> findByRanchId(UUID ranchId, Pageable pageable);
    Page<PastoralBovinEntity> findByStatut(StatutBovin statut, Pageable pageable);
    long countByRanchIdAndStatut(UUID ranchId, StatutBovin statut);
    long countByCategorieCodeAndStatut(String categorieCode, StatutBovin statut);
    long countByStatut(StatutBovin statut);

    @Query("SELECT COALESCE(SUM(b.valeurComptableActuelle), 0) FROM PastoralBovinEntity b WHERE b.statut = :statut")
    BigDecimal sumValeurComptableByStatut(@Param("statut") StatutBovin statut);

    @Query("SELECT b.categorieCode, COUNT(b) FROM PastoralBovinEntity b WHERE b.statut = :statut GROUP BY b.categorieCode")
    List<Object[]> countGroupedByCategorieAndStatut(@Param("statut") StatutBovin statut);

    @Query("SELECT b.ranch.id, COUNT(b) FROM PastoralBovinEntity b WHERE b.statut = :statut GROUP BY b.ranch.id")
    List<Object[]> countGroupedByRanchAndStatut(@Param("statut") StatutBovin statut);

    @Query("SELECT COUNT(b) FROM PastoralBovinEntity b WHERE b.statut = :statut AND b.dateNaissance >= :debut AND b.dateNaissance <= :fin")
    long countNaissancesByStatutAndPeriode(
            @Param("statut") StatutBovin statut,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);
}
