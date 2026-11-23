package com.sodepa.erp.budgetaire.source.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/budget/sources-financement/{id}/init_update} :
 * l'état voulu du libellé et du bailleur. Le code et le type sont figés.
 *
 * @param libelle  nouveau libellé
 * @param bailleur nouveau bailleur, absent pour l'effacer (hors partenaire)
 */
public record ModifierSourceRequest(
        @NotBlank(message = "Indiquez le libellé de la source.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String libelle,
        @Size(max = 255, message = "Le nom du bailleur ne doit pas dépasser 255 caractères.")
        String bailleur
) {
}
