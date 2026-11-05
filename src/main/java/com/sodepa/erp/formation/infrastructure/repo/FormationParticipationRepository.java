package com.sodepa.erp.formation.infrastructure.repo;

import com.sodepa.erp.formation.infrastructure.entities.FormationParticipationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormationParticipationRepository extends JpaRepository<FormationParticipationEntity, UUID> {
    List<FormationParticipationEntity> findByActionId(UUID actionId);
    List<FormationParticipationEntity> findByEmployeId(UUID employeId);
}
