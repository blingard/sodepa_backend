package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.AuditTrailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux logs d'audit (audit trail).
 */
@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrailEntity, UUID> {

    /**
     * Recherche l'historique d'audit d'une entité spécifique par son type et son ID.
     * 
     * @param entiteNom le nom de la table ou de la classe de l'entité
     * @param entiteId l'identifiant de l'enregistrement de l'entité
     * @return la liste ordonnée des logs d'audit associés
     */
    List<AuditTrailEntity> findByEntiteNomAndEntiteIdOrderByTimestampDesc(String entiteNom, UUID entiteId);
}
