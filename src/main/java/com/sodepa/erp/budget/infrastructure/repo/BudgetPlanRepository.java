package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des plans budgétaires annuels.
 */
@Repository
public interface BudgetPlanRepository extends JpaRepository<BudgetPlanEntity, UUID> {

    /**
     * Recherche les versions d'un plan budgétaire pour un exercice donné.
     * 
     * @param annee l'exercice concerné
     * @return la liste des plans budgétaires de cette année
     */
    List<BudgetPlanEntity> findByAnnee(int annee);

    /**
     * Recherche la version spécifique d'un budget pour un exercice donné.
     * 
     * @param annee l'exercice concerné
     * @param version le numéro de version
     * @return un Optional contenant le plan budgétaire correspondant
     */
    Optional<BudgetPlanEntity> findByAnneeAndVersion(int annee, int version);
}
