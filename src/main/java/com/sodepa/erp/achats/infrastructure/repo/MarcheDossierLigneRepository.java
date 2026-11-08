package com.sodepa.erp.achats.infrastructure.repo;

import com.sodepa.erp.achats.infrastructure.entities.MarcheDossierLigneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `MarcheDossierLigneRepository`.
 */
@Repository
public interface MarcheDossierLigneRepository extends JpaRepository<MarcheDossierLigneEntity, UUID> {
    List<MarcheDossierLigneEntity> findByDossierIdOrderByOrdreAsc(UUID dossierId);
}
