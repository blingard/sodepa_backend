package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.EvolutionStructureOrganigrammeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour evolution structure organigramme (ORGANIGRAMME).
 */
@Repository
public interface EvolutionStructureOrganigrammeRepository extends JpaRepository<EvolutionStructureOrganigrammeEntity, UUID> {
    List<EvolutionStructureOrganigrammeEntity> findByCodeUniteIgnoreCaseOrderByDateEffetDesc(String codeUnite);
    Optional<EvolutionStructureOrganigrammeEntity> findFirstByCodeUniteIgnoreCaseAndActifTrueOrderByDateEffetDesc(String codeUnite);
    boolean existsByCodeUniteIgnoreCase(String codeUnite);
}
