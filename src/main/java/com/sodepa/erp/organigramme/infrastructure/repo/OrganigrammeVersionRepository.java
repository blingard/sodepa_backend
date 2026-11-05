package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.OrganigrammeVersionEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutVersionOrganigramme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganigrammeVersionRepository extends JpaRepository<OrganigrammeVersionEntity, UUID> {

    Optional<OrganigrammeVersionEntity> findByNumeroVersion(String numeroVersion);

    boolean existsByNumeroVersion(String numeroVersion);

    List<OrganigrammeVersionEntity> findByStatut(StatutVersionOrganigramme statut);

    Optional<OrganigrammeVersionEntity> findFirstByStatutOrderByDateEffetDesc(StatutVersionOrganigramme statut);
}
