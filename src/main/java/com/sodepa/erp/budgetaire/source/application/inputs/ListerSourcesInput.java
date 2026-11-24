package com.sodepa.erp.budgetaire.source.application.inputs;

import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import org.springframework.data.domain.Pageable;

/**
 * Liste des sources de financement, triée par code.
 *
 * @param pageable         page demandée, plafonnée à 100 lignes
 * @param type             filtre facultatif
 * @param inclureInactives vrai pour montrer aussi les sources désactivées
 */
public record ListerSourcesInput(Pageable pageable, TypeSourceFinancement type, boolean inclureInactives) {
}
