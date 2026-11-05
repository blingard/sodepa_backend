package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralRanchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastoralRanchRepository extends JpaRepository<PastoralRanchEntity, UUID> {
    Optional<PastoralRanchEntity> findByCode(String code);
    List<PastoralRanchEntity> findByActifTrue();
}
