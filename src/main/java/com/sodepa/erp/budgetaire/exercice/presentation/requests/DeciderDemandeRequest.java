package com.sodepa.erp.budgetaire.exercice.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/budget/exercices/validate_or_reject/{requestId}}.
 *
 * <p>Contrairement à {@code RejectOrValidateSubmitRequest}, il ne demande pas
 * le type d'opération : le serveur le connaît déjà par la demande. Le motif
 * n'est exigé que pour un rejet, contrôle fait par l'adaptateur.</p>
 *
 * @param decision {@code ACCEPTED} ou {@code REJECTED}
 * @param notes    commentaire, obligatoire en cas de rejet
 */
public record DeciderDemandeRequest(
        @NotNull(message = "Indiquez la décision.")
        MakerCheckerStatus decision,
        @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1 000 caractères.")
        String notes
) {
}
