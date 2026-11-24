package com.sodepa.erp.budgetaire.nature.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/budget/natures/{id}/init_update} : seul le libellé
 * se modifie ; le code, la section, le sens et le compte sont figés.
 *
 * @param libelle nouveau libellé
 */
public record ModifierNatureRequest(
        @NotBlank(message = "Indiquez le libellé de la nature.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String libelle
) {
}
