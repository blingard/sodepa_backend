package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.EcheanceFinancementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux échéances de remboursement de financements.
 */
@Repository
public interface EcheanceFinancementRepository extends JpaRepository<EcheanceFinancementEntity, UUID> {

    /**
     * Recherche les échéances de remboursement par ligne de financement.
     * 
     * @param ligneFinancementId l'identifiant du financement
     * @return la liste des échéances
     */
    List<EcheanceFinancementEntity> findByLigneFinancementId(UUID ligneFinancementId);

    /**
     * Recherche les échéances à payer sur un intervalle de dates donné.
     * 
     * @param debut la date de début de l'intervalle
     * @param fin la date de fin de l'intervalle
     * @param statut le statut de l'échéance (ex: 'A_PAYER')
     * @return la liste des échéances
     */
    List<EcheanceFinancementEntity> findByDateEcheanceBetweenAndStatut(LocalDate debut, LocalDate fin, String statut);
}
