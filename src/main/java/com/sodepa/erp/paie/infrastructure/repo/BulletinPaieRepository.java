package com.sodepa.erp.paie.infrastructure.repo;

import com.sodepa.erp.paie.infrastructure.entities.BulletinPaieEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `BulletinPaieRepository`.
 */
@Repository
public interface BulletinPaieRepository extends JpaRepository<BulletinPaieEntity, UUID> {
    List<BulletinPaieEntity> findByPeriodeId(UUID periodeId);
    Page<BulletinPaieEntity> findByPeriodeId(UUID periodeId, Pageable pageable);
    Optional<BulletinPaieEntity> findByPeriodeIdAndEmployeId(UUID periodeId, UUID employeId);
    List<BulletinPaieEntity> findByEmployeIdOrderByPeriodeDateDebutDesc(UUID employeId);
}
