package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.BudgetEngagementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des engagements budgétaires.
 */
@Repository
public interface BudgetEngagementRepository extends JpaRepository<BudgetEngagementEntity, UUID> {

    /**
     * Recherche un engagement par son numéro unique.
     * 
     * @param numeroEngagement le numéro de bon de commande ou d'engagement
     * @return un Optional contenant l'engagement correspondant
     */
    Optional<BudgetEngagementEntity> findByNumeroEngagement(String numeroEngagement);

    /**
     * Recherche les engagements associés à une ligne budgétaire.
     * 
     * @param budgetItemId l'identifiant de la ligne budgétaire
     * @return la liste des engagements associés
     */
    List<BudgetEngagementEntity> findByBudgetItemId(UUID budgetItemId);
}
