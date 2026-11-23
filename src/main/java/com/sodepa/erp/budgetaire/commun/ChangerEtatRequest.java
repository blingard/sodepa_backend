package com.sodepa.erp.budgetaire.commun;

import jakarta.validation.constraints.Size;

/**
 * Corps des routes {@code init_deactivate} et {@code init_reactivate} des référentiels budgétaires.
 *
 * <p>Le motif est exigé pour une désactivation (contrôle de l'adaptateur),
 * facultatif pour une réactivation.</p>
 *
 * @param motif raison du changement d'état
 */
public record ChangerEtatRequest(
        @Size(max = 1000, message = "Le motif ne doit pas dépasser 1 000 caractères.")
        String motif
) {
}
