package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour les demandes Maker-Checker et leur synchronisation avec le moteur de workflow.
 */
@Repository
public interface MakerCheckerRequestJpaRepo extends JpaRepository<MakerCheckerRequestEntity, UUID> {

    Page<MakerCheckerRequestEntity> findAllByStatus(MakerCheckerStatus status, Pageable pageable);

    Page<MakerCheckerRequestEntity> findAllByEntityName(MakerCheckerEntityName entityName, Pageable pageable);

    Page<MakerCheckerRequestEntity> findAllByEntityNameAndStatus(
            MakerCheckerEntityName entityName, MakerCheckerStatus status, Pageable pageable);

    /**
     * Demandes qu'un acteur donné peut trancher : celles qu'il n'a pas soumises.
     *
     * <p>La séparation maker-checker est vérifiée à la décision ; l'appliquer
     * dès la liste évite de proposer une action vouée au refus.</p>
     */
    Page<MakerCheckerRequestEntity> findAllByEntityNameAndStatusAndMakerIdNot(
            MakerCheckerEntityName entityName, MakerCheckerStatus status,
            String makerId, Pageable pageable);

    Optional<MakerCheckerRequestEntity> findByEntityPkAndEntityName(String entityPk, MakerCheckerEntityName entityName);

    Optional<MakerCheckerRequestEntity> findByWorkflowInstanceId(UUID workflowInstanceId);

    List<MakerCheckerRequestEntity> findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
            String entityPk, MakerCheckerEntityName entityName, MakerCheckerStatus status);
}
