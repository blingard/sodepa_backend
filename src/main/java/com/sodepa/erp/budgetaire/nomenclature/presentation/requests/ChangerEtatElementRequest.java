package com.sodepa.erp.budgetaire.nomenclature.presentation.requests;

import jakarta.validation.constraints.Size;

/**
 * Corps de {@code init_deactivate} et {@code init_reactivate}.
 *
 * <p>Le motif est exigé pour une désactivation (contrôle de l'adaptateur),
 * facultatif pour une réactivation.</p>
 *
 * @param motif raison du changement d'état
 */
public record ChangerEtatElementRequest(
        @Size(max = 1000, message = "Le motif ne doit pas dépasser 1 000 caractères.")
        String motif
) {
}
