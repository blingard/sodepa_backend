package com.sodepa.erp.budgetaire.nature.application.inputs;

import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import org.springframework.data.domain.Pageable;

/**
 * Liste des natures, triée par code.
 *
 * @param pageable         page demandée, plafonnée à 100 lignes
 * @param section          filtre facultatif
 * @param sens             filtre facultatif
 * @param inclureInactives vrai pour montrer aussi les natures désactivées
 */
public record ListerNaturesInput(Pageable pageable, SectionBudgetaire section, SensBudgetaire sens,
                                 boolean inclureInactives) {
}
