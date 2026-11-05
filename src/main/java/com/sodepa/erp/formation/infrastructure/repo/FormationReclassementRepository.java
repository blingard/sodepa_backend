package com.sodepa.erp.formation.infrastructure.repo;

import com.sodepa.erp.formation.infrastructure.entities.FormationReclassementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormationReclassementRepository extends JpaRepository<FormationReclassementEntity, UUID> {
    List<FormationReclassementEntity> findByEmployeId(UUID employeId);
    List<FormationReclassementEntity> findByValideDgFalse();
}
