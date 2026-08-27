package com.sodepa.erp.budget.application.inputs;

import com.sodepa.erp.budget.infrastructure.entities.StatutBudget;
import org.springframework.data.domain.Pageable;

/**
 * Critères de recherche des plans budgétaires.
 *
 * @param pageable pagination et tri
 * @param annee exercice visé ; {@code null} pour tous
 * @param statut état du plan ; {@code null} pour tous
 */
public record RechercheBudgetPlanInput(
    Pageable pageable,
    Integer annee,
    StatutBudget statut
) {
}
