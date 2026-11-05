package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralHerdCompositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastoralHerdCompositionRepository extends JpaRepository<PastoralHerdCompositionEntity, UUID> {
    List<PastoralHerdCompositionEntity> findByAnneeExerciceOrderByDateInventaireDesc(Integer annee);
    Optional<PastoralHerdCompositionEntity> findByRanchIdAndAnneeExercice(UUID ranchId, Integer annee);
}
