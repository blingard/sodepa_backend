package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.SchemaEcritureComptableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour schema ecriture comptable (COMPTABILITE).
 */
@Repository
public interface SchemaEcritureComptableRepository extends JpaRepository<SchemaEcritureComptableEntity, UUID> {
    Optional<SchemaEcritureComptableEntity> findByCodeIgnoreCase(String code);
    List<SchemaEcritureComptableEntity> findByActifTrueOrderByCodeAsc();
}
