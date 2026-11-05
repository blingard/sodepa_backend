package com.sodepa.erp.paie.infrastructure.repo;

import com.sodepa.erp.paie.infrastructure.entities.BulletinLigneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulletinLigneRepository extends JpaRepository<BulletinLigneEntity, UUID> {
    List<BulletinLigneEntity> findByBulletinIdOrderByOrdreAsc(UUID bulletinId);
}
