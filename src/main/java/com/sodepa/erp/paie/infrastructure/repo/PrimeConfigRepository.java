package com.sodepa.erp.paie.infrastructure.repo;

import com.sodepa.erp.paie.infrastructure.entities.PrimeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrimeConfigRepository extends JpaRepository<PrimeConfigEntity, UUID> {
    Optional<PrimeConfigEntity> findByCode(String code);
    List<PrimeConfigEntity> findByActifTrue();
}
