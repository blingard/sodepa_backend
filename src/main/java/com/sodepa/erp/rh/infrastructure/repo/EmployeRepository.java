package com.sodepa.erp.rh.infrastructure.repo;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `EmployeRepository`.
 */
@Repository
public interface EmployeRepository extends JpaRepository<EmployeEntity, UUID>, JpaSpecificationExecutor<EmployeEntity> {
    Optional<EmployeEntity> findByMatricule(String matricule);
    Optional<EmployeEntity> findByUtilisateurId(UUID utilisateurId);
    boolean existsByUtilisateurId(UUID utilisateurId);
    List<EmployeEntity> findByUniteOrganisationnelleId(UUID uniteId);
    List<EmployeEntity> findByPosteId(UUID posteId);
    List<EmployeEntity> findByStatutActivite(StatutActivite statut);
    long countByStatutActivite(StatutActivite statut);
}
