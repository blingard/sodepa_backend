package com.sodepa.erp.budget.application.inputs;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Critères de recherche des engagements budgétaires.
 *
 * @param pageable pagination et tri
 * @param planId plan dont on veut les engagements ; {@code null} pour tous
 * @param statut ENGAGED, CONVERTED_TO_REAL ou CANCELLED ; {@code null} pour tous
 */
public record RechercheEngagementInput(
    Pageable pageable,
    UUID planId,
    String statut
) {
}
