package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.ContratCouvertureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux contrats de couverture de change.
 */
@Repository
public interface ContratCouvertureRepository extends JpaRepository<ContratCouvertureEntity, UUID> {

    /**
     * Recherche les contrats de couverture actifs pour une devise donnée.
     * 
     * @param devise la devise étrangère (ex: 'USD')
     * @param statut le statut du contrat (ex: 'ACTIF')
     * @return la liste des contrats de couverture actifs
     */
    List<ContratCouvertureEntity> findByDeviseCibleAndStatut(String devise, String statut);
}
