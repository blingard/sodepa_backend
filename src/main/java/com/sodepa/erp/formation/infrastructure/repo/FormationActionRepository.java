package com.sodepa.erp.formation.infrastructure.repo;

import com.sodepa.erp.formation.infrastructure.entities.FormationActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormationActionRepository extends JpaRepository<FormationActionEntity, UUID> {
    Optional<FormationActionEntity> findByCode(String code);
    List<FormationActionEntity> findByPlanId(UUID planId);
}
