package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutPoste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `PosteRepository`.
 */
@Repository
public interface PosteRepository extends JpaRepository<PosteEntity, UUID> {

    Optional<PosteEntity> findByCodePoste(String codePoste);

    boolean existsByCodePoste(String codePoste);

    List<PosteEntity> findByUniteOrganisationnelleId(UUID uniteId);

    List<PosteEntity> findByPosteSuperieurId(UUID posteId);

    List<PosteEntity> findAllByStatut(StatutPoste statut);
}
