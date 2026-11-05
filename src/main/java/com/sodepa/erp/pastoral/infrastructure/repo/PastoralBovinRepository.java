package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralBovinEntity;
import com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastoralBovinRepository extends JpaRepository<PastoralBovinEntity, UUID> {
    Optional<PastoralBovinEntity> findByMatriculeBoucle(String matriculeBoucle);
    List<PastoralBovinEntity> findByRanchIdAndStatut(UUID ranchId, StatutBovin statut);
    Page<PastoralBovinEntity> findByRanchId(UUID ranchId, Pageable pageable);
    Page<PastoralBovinEntity> findByStatut(StatutBovin statut, Pageable pageable);
    long countByRanchIdAndStatut(UUID ranchId, StatutBovin statut);
    long countByCategorieCodeAndStatut(String categorieCode, StatutBovin statut);
    long countByStatut(StatutBovin statut);
}
