package com.sodepa.erp.budgetaire.structure.application.inputs;

import org.springframework.data.domain.Pageable;

/**
 * Liste des structures.
 *
 * @param pageable        page demandée, plafonnée à 100 lignes
 * @param inclureInactives vrai pour montrer aussi les structures désactivées
 */
public record ListerStructuresInput(Pageable pageable, boolean inclureInactives) {
}
