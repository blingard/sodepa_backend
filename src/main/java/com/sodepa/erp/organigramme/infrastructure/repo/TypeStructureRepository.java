package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.TypeStructureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `TypeStructureRepository`.
 */
@Repository
public interface TypeStructureRepository extends JpaRepository<TypeStructureEntity, UUID> {

    Optional<TypeStructureEntity> findByCode(String code);

    List<TypeStructureEntity> findAllByOrderByNiveauHierarchiqueAscLibelleAsc();

    List<TypeStructureEntity> findByActifTrueOrderByNiveauHierarchiqueAscLibelleAsc();

    List<TypeStructureEntity> findByTypeParentIsNull();

    List<TypeStructureEntity> findByTypeParent_Id(UUID parentId);

    boolean existsByCode(String code);
}
