package com.sodepa.erp.budgetaire.structure.presentation.requests;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Corps de {@code POST /api/budget/structures/init_create}.
 *
 * <p>Trois identifiants de l'organigramme, jamais des libellés : l'écran les
 * choisit dans l'arbre des unités et dans les postes de l'unité retenue.</p>
 *
 * @param uniteId                  unité à déclarer (Direction Générale, ranch…)
 * @param ordonnateurPosteId       poste de l'ordonnateur, rattaché à cette unité
 * @param serviceEngagementUniteId unité du service d'engagement, dans son sous-arbre
 */
public record CreerStructureRequest(
        @NotNull(message = "Choisissez l'unité de l'organigramme à déclarer.")
        UUID uniteId,
        @NotNull(message = "Désignez le poste de l'ordonnateur.")
        UUID ordonnateurPosteId,
        @NotNull(message = "Désignez le service d'engagement.")
        UUID serviceEngagementUniteId
) {
}
