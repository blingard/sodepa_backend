package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.ContratCouvertureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Portefeuille de couvertures, filtré au besoin.
     *
     * <p>
     * Les deux critères sont facultatifs. Sans cette requête, un contrat
     * n'était visible qu'à l'instant de son enregistrement, alors que son
     * évaluation au cours du jour réclame son identifiant.
     * </p>
     */
    @Query("""
            SELECT c FROM ContratCouvertureEntity c
            WHERE (:devise IS NULL OR c.deviseCible = :devise)
              AND (:statut IS NULL OR c.statut = :statut)
            ORDER BY c.dateEcheance ASC
            """)
    List<ContratCouvertureEntity> rechercher(
            @Param("devise") String devise,
            @Param("statut") String statut);
}
