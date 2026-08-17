package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.ReleveBancaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux relevés bancaires importés.
 */
@Repository("budgetReleveBancaireRepository")
public interface ReleveBancaireRepository extends JpaRepository<ReleveBancaireEntity, UUID> {

    /**
     * Recherche les relevés importés pour une banque spécifique.
     * 
     * @param banqueId l'identifiant de la banque partenaire
     * @return la liste des relevés bancaires de cette banque
     */
    List<ReleveBancaireEntity> findByBanqueId(UUID banqueId);
}
