package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.ReferentielSanitaireVeterinaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour referentiel sanitaire veterinaire (PASTORAL).
 */
@Repository
public interface ReferentielSanitaireVeterinaireRepository extends JpaRepository<ReferentielSanitaireVeterinaireEntity, UUID> {
    List<ReferentielSanitaireVeterinaireEntity> findByActifTrueOrderByCodeAsc();
    List<ReferentielSanitaireVeterinaireEntity> findByTypeElementIgnoreCaseAndActifTrueOrderByCodeAsc(String typeElement);
    Optional<ReferentielSanitaireVeterinaireEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
