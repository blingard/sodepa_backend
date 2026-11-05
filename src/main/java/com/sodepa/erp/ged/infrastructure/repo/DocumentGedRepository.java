package com.sodepa.erp.ged.infrastructure.repo;

import com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentGedRepository extends JpaRepository<DocumentGedEntity, UUID> {
    List<DocumentGedEntity> findByTypeEntiteAndEntiteIdOrderByDateUploadDesc(String typeEntite, UUID entiteId);
    List<DocumentGedEntity> findByHashSha256(String hashSha256);
}
