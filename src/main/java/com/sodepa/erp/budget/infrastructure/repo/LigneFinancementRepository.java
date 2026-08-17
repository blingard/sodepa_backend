package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.LigneFinancementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux lignes de financement (prêts, obligataires, leasings).
 */
@Repository
public interface LigneFinancementRepository extends JpaRepository<LigneFinancementEntity, UUID> {

    /**
     * Recherche les financements contractés auprès d'une banque donnée.
     * 
     * @param banqueId l'identifiant unique de la banque
     * @return la liste des lignes de financement
     */
    List<LigneFinancementEntity> findByBanqueId(UUID banqueId);

    /**
     * Recherche les financements par type (ex: PRET, LEASING, OBLIGATION).
     * 
     * @param type le type de financement recherché
     * @return la liste des lignes de ce type
     */
    List<LigneFinancementEntity> findByType(String type);
}
