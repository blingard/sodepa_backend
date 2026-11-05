package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralCategorieCheptelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastoralCategorieCheptelRepository extends JpaRepository<PastoralCategorieCheptelEntity, UUID> {
    Optional<PastoralCategorieCheptelEntity> findByCode(String code);
}
