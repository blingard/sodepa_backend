package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.EngagementHorsBilanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux engagements hors-bilan.
 */
@Repository
public interface EngagementHorsBilanRepository extends JpaRepository<EngagementHorsBilanEntity, UUID> {

    /**
     * Recherche les engagements d'un tiers spécifique.
     * 
     * @param tiersId l'identifiant unique du tiers
     * @return la liste des engagements hors-bilan associés
     */
    List<EngagementHorsBilanEntity> findByTiersId(UUID tiersId);

    /**
     * Recherche les engagements par type (ex: 'LEASING', 'GARANTIE_BANCAIRE').
     * 
     * @param type le type recherché
     * @return la liste des engagements correspondants
     */
    List<EngagementHorsBilanEntity> findByType(String type);
}
