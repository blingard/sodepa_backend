package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.ReferentielAbattageTransformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour referentiel abattage transformation (PASTORAL).
 */
@Repository
public interface ReferentielAbattageTransformationRepository extends JpaRepository<ReferentielAbattageTransformationEntity, UUID> {
    List<ReferentielAbattageTransformationEntity> findByActifTrueOrderByCodeAsc();
    List<ReferentielAbattageTransformationEntity> findByTypeNormeIgnoreCaseAndActifTrueOrderByCodeAsc(String typeNorme);
    Optional<ReferentielAbattageTransformationEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
