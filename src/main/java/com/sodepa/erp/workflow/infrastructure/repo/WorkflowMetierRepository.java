package com.sodepa.erp.workflow.infrastructure.repo;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowMetierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface Spring Data JPA pour le référentiel des opérations / métiers du workflow.
 */
@Repository
public interface WorkflowMetierRepository extends JpaRepository<WorkflowMetierEntity, UUID> {

    /**
     * Recherche une opération métier par son code unique en anglais (ex: "RECRUITMENT", "SERVICE_MEMO").
     *
     * @param code Le code fonctionnel.
     * @return Optional contenant le métier trouvé.
     */
    Optional<WorkflowMetierEntity> findByCode(String code);

    /**
     * Vérifie si un métier existe déjà pour un code donné.
     *
     * @param code Le code fonctionnel.
     * @return true si existant.
     */
    boolean existsByCode(String code);

    /**
     * Récupère tous les métiers actifs associés à un domaine fonctionnel.
     *
     * @param domain Le domaine fonctionnel (RH, BUDGET, etc.).
     * @return Liste des opérations métiers actives.
     */
    List<WorkflowMetierEntity> findByDomainAndIsActiveTrue(WorkflowDomaine domain);

    /**
     * Récupère la totalité des métiers actuellement actifs dans le système.
     *
     * @return Liste de toutes les opérations métiers actives.
     */
    Page<WorkflowMetierEntity> findAllByIsActiveTrue(Pageable pageable);
}
