package com.sodepa.erp.organigramme.infrastructure.repo;

import com.sodepa.erp.organigramme.infrastructure.entities.DelegationPouvoirEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDelegation;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `DelegationPouvoirRepository`.
 */
@Repository
public interface DelegationPouvoirRepository extends JpaRepository<DelegationPouvoirEntity, UUID> {

    List<DelegationPouvoirEntity> findByDelegantIdAndStatut(UUID delegantId, StatutDelegation statut);

    List<DelegationPouvoirEntity> findByDelegataireIdAndStatut(UUID delegataireId, StatutDelegation statut);

    @Query("SELECT d FROM DelegationPouvoirEntity d WHERE d.delegant.id = :delegantId " +
           "AND d.statut = 'ACTIVE' " +
           "AND :moment BETWEEN d.dateDebut AND d.dateFin " +
           "AND (d.typeDelegation = :typeDelegation OR d.typeDelegation = 'TOTALE')")
    List<DelegationPouvoirEntity> findActiveDelegations(
            @Param("delegantId") UUID delegantId,
            @Param("moment") Instant moment,
            @Param("typeDelegation") TypeDelegation typeDelegation);
}
