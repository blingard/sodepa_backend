package com.sodepa.erp.budgetaire.structure.presentation.requests;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Corps de {@code POST /api/budget/structures/{id}/init_update} : l'état voulu
 * de l'ordonnateur et du service d'engagement. L'unité ne change pas.
 *
 * @param ordonnateurPosteId       poste de l'ordonnateur
 * @param serviceEngagementUniteId unité du service d'engagement
 */
public record ModifierStructureRequest(
        @NotNull(message = "Désignez le poste de l'ordonnateur.")
        UUID ordonnateurPosteId,
        @NotNull(message = "Désignez le service d'engagement.")
        UUID serviceEngagementUniteId
) {
}
