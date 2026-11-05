package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutUnite;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniteOrganisationnelleRepository extends JpaRepository<UniteOrganisationnelleEntity, UUID> {

    Optional<UniteOrganisationnelleEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<UniteOrganisationnelleEntity> findByParentIsNull();

    List<UniteOrganisationnelleEntity> findByParentId(UUID parentId);

    List<UniteOrganisationnelleEntity> findAllByStatut(StatutUnite statut);

    @Query("SELECT u FROM UniteOrganisationnelleEntity u WHERE u.materializedPath LIKE CONCAT(:pathPrefix, '%') ORDER BY u.materializedPath ASC")
    List<UniteOrganisationnelleEntity> findDescendantsByPathPrefix(@Param("pathPrefix") String pathPrefix);
}
