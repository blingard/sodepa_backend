package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

import com.sodepa.erp.utils.CodeJournal;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntity, UUID> {
    Optional<JournalEntity> findByCode(CodeJournal code);
}
