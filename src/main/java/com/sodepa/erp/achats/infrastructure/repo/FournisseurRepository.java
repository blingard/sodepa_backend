package com.sodepa.erp.achats.infrastructure.repo;

import com.sodepa.erp.achats.infrastructure.entities.FournisseurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FournisseurRepository extends JpaRepository<FournisseurEntity, UUID> {
    Optional<FournisseurEntity> findByCode(String code);
    List<FournisseurEntity> findByAgreeSodepaTrueAndActifTrue();
    List<FournisseurEntity> findByActifTrue();
}
