package com.sodepa.erp.rh.infrastructure.repo;

import com.sodepa.erp.rh.infrastructure.entities.GrilleSalarialeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrilleSalarialeRepository extends JpaRepository<GrilleSalarialeEntity, UUID> {
    Optional<GrilleSalarialeEntity> findByCategorieAndEchelon(Integer categorie, String echelon);
    List<GrilleSalarialeEntity> findByOrderByCategorieAscEchelonAsc();
    List<GrilleSalarialeEntity> findByGroupeOrderByCategorieAscEchelonAsc(Integer groupe);
}
