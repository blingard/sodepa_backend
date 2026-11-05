package com.sodepa.erp.rh.infrastructure.repo;

import com.sodepa.erp.rh.infrastructure.entities.ContratTravailEntity;
import com.sodepa.erp.rh.infrastructure.entities.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratTravailRepository extends JpaRepository<ContratTravailEntity, UUID> {
    List<ContratTravailEntity> findByEmployeId(UUID employeId);
    Optional<ContratTravailEntity> findByNumeroContrat(String numeroContrat);
    List<ContratTravailEntity> findByStatut(StatutContrat statut);
    Optional<ContratTravailEntity> findByWorkflowInstanceId(UUID workflowInstanceId);
    Optional<ContratTravailEntity> findFirstByEmployeIdOrderByDateCreationDesc(UUID employeId);
}
