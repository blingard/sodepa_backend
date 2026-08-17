package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetDemandeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux propositions budgétaires collaboratives.
 */
@Repository
public interface BudgetDemandeRepository extends JpaRepository<BudgetDemandeEntity, UUID> {

    /**
     * Recherche toutes les propositions budgétaires d'un département pour une année spécifique.
     * 
     * @param departementId l'identifiant du département
     * @param annee l'exercice budgétaire
     * @return la liste des propositions budgétaires associées
     */
    List<BudgetDemandeEntity> findByDepartementIdAndAnnee(UUID departementId, Integer annee);

    /**
     * Recherche toutes les propositions d'une année spécifique.
     * 
     * @param annee l'exercice budgétaire
     * @return la liste des propositions
     */
    List<BudgetDemandeEntity> findByAnnee(Integer annee);
}
