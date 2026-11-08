package com.sodepa.erp.dashboard.infrastructure.repo;

import com.sodepa.erp.dashboard.infrastructure.entities.ModeleDocumentNumMasqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour modele document num masque (DASHBOARD).
 */
@Repository
public interface ModeleDocumentNumMasqueRepository extends JpaRepository<ModeleDocumentNumMasqueEntity, UUID> {
    List<ModeleDocumentNumMasqueEntity> findByActifTrueOrderByCodeModeleAsc();
    Optional<ModeleDocumentNumMasqueEntity> findByCodeModeleIgnoreCase(String codeModele);
    boolean existsByCodeModeleIgnoreCase(String codeModele);
}
