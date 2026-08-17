package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MakerCheckerRequestJpaRepo extends JpaRepository<MakerCheckerRequestEntity, UUID> {
    Page<MakerCheckerRequestEntity> findAllByStatus(MakerCheckerStatus status, Pageable pageable);

    Optional<MakerCheckerRequestEntity> findByEntityPkAndEntityName(String entityPk, MakerCheckerEntityName entityName);
}
