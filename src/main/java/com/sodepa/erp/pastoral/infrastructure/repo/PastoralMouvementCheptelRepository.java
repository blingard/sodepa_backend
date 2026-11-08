package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralMouvementCheptelEntity;
import com.sodepa.erp.pastoral.infrastructure.entities.TypeMouvementCheptel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `PastoralMouvementCheptelRepository`.
 */
@Repository
public interface PastoralMouvementCheptelRepository extends JpaRepository<PastoralMouvementCheptelEntity, UUID> {
    Optional<PastoralMouvementCheptelEntity> findByNumeroMouvement(String numeroMouvement);
    List<PastoralMouvementCheptelEntity> findByBovinIdOrderByDateMouvementDesc(UUID bovinId);
    Page<PastoralMouvementCheptelEntity> findByTypeMouvement(TypeMouvementCheptel type, Pageable pageable);
    List<PastoralMouvementCheptelEntity> findByRanchSourceIdOrderByDateMouvementDesc(UUID ranchId);
}
