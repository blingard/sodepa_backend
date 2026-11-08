package com.sodepa.erp.paie.infrastructure.repo;

import com.sodepa.erp.paie.infrastructure.entities.VariablePaieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `VariablePaieRepository`.
 */
@Repository
public interface VariablePaieRepository extends JpaRepository<VariablePaieEntity, UUID> {
    List<VariablePaieEntity> findByPeriodeId(UUID periodeId);
    List<VariablePaieEntity> findByPeriodeIdAndEmployeId(UUID periodeId, UUID employeId);
}
