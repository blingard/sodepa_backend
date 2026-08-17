package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ImmobilisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImmobilisationRepository extends JpaRepository<ImmobilisationEntity, UUID> {
    Optional<ImmobilisationEntity> findByCode(String code);
}
