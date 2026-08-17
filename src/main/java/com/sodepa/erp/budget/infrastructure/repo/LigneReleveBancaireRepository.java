package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.LigneReleveBancaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux lignes de transactions de relevés bancaires.
 */
@Repository("budgetLigneReleveBancaireRepository")
public interface LigneReleveBancaireRepository extends JpaRepository<LigneReleveBancaireEntity, UUID> {

    /**
     * Recherche les lignes de transaction associées à un relevé donné.
     * 
     * @param releveId l'identifiant du relevé
     * @return la liste des lignes correspondantes
     */
    List<LigneReleveBancaireEntity> findByReleveBancaireId(UUID releveId);

    /**
     * Recherche les lignes par leur statut de rapprochement (ex: 'NON_RAPPROCHE').
     * 
     * @param statutRapprochement le statut recherché
     * @return la liste des lignes correspondantes
     */
    List<LigneReleveBancaireEntity> findByStatutRapprochement(String statutRapprochement);
}
