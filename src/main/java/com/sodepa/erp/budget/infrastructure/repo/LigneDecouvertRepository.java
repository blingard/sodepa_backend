package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.LigneDecouvertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux lignes de découvert.
 */
@Repository
public interface LigneDecouvertRepository extends JpaRepository<LigneDecouvertEntity, UUID> {

    /**
     * Recherche les lignes de découvert associées à une banque spécifique.
     * 
     * @param banqueId l'identifiant unique de la banque
     * @return la liste des lignes de découvert de cette banque
     */
    List<LigneDecouvertEntity> findByBanqueId(UUID banqueId);
}
