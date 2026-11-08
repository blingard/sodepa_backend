package com.sodepa.erp.formation.infrastructure.repo;

import com.sodepa.erp.formation.infrastructure.entities.StageEntity;
import com.sodepa.erp.formation.infrastructure.entities.StatutStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `StageRepository`.
 */
@Repository
public interface StageRepository extends JpaRepository<StageEntity, UUID> {
    Optional<StageEntity> findByCodeDossier(String codeDossier);
    Page<StageEntity> findByStatut(StatutStage statut, Pageable pageable);
}
