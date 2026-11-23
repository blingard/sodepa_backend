package com.sodepa.erp.budgetaire.nomenclature.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Corps de {@code POST /api/budget/nomenclature/{id}/init_update}.
 *
 * <p>Porte l'état voulu des seuls champs modifiables : un champ absent est
 * vidé. Le code, le niveau et le parent ne se modifient pas.</p>
 *
 * @param libelle       nouveau libellé
 * @param description   nouvelle description, absente pour l'effacer
 * @param responsableId nouveau responsable ; obligatoire pour un programme
 */
public record ModifierElementRequest(
        @NotBlank(message = "Indiquez le libellé de l'élément.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String libelle,
        @Size(max = 1000, message = "La description ne doit pas dépasser 1 000 caractères.")
        String description,
        UUID responsableId
) {
}
