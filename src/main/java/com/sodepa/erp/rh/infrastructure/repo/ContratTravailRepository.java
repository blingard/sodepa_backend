package com.sodepa.erp.rh.infrastructure.repo;

import com.sodepa.erp.rh.infrastructure.entities.ContratTravailEntity;
import com.sodepa.erp.rh.infrastructure.entities.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `ContratTravailRepository`.
 */
@Repository
public interface ContratTravailRepository extends JpaRepository<ContratTravailEntity, UUID> {
    List<ContratTravailEntity> findByEmployeId(UUID employeId);
    Optional<ContratTravailEntity> findByNumeroContrat(String numeroContrat);
    List<ContratTravailEntity> findByStatut(StatutContrat statut);
    Optional<ContratTravailEntity> findByWorkflowInstanceId(UUID workflowInstanceId);
    Optional<ContratTravailEntity> findFirstByEmployeIdOrderByDateCreationDesc(UUID employeId);

    @Query("SELECT c FROM ContratTravailEntity c JOIN FETCH c.employe e JOIN FETCH e.utilisateur u " +
           "WHERE u.actif = true AND (c.dateFinContrat < :today OR c.statut IN :statuts)")
    List<ContratTravailEntity> findContratsEchusPourUtilisateursActifs(
            @Param("today") LocalDate today,
            @Param("statuts") Collection<StatutContrat> statuts);
}
