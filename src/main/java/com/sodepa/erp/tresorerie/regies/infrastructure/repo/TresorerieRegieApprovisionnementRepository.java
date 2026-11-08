package com.sodepa.erp.tresorerie.regies.infrastructure.repo;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.StatutApprovisionnementRegie;
import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TresorerieRegieApprovisionnementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `TresorerieRegieApprovisionnementRepository`.
 */
@Repository
public interface TresorerieRegieApprovisionnementRepository extends JpaRepository<TresorerieRegieApprovisionnementEntity, UUID> {
    Optional<TresorerieRegieApprovisionnementEntity> findByNumeroDemande(String numeroDemande);
    List<TresorerieRegieApprovisionnementEntity> findByRegieIdOrderByDateDemandeDesc(UUID regieId);
    Page<TresorerieRegieApprovisionnementEntity> findByStatut(StatutApprovisionnementRegie statut, Pageable pageable);
}
