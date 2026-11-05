package com.sodepa.erp.formation.infrastructure.repo;

import com.sodepa.erp.formation.infrastructure.entities.FormationPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormationPlanRepository extends JpaRepository<FormationPlanEntity, UUID> {
    Optional<FormationPlanEntity> findByAnneeExercice(Integer anneeExercice);
}
