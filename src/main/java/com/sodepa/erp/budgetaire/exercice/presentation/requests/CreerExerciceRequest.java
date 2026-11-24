package com.sodepa.erp.budgetaire.exercice.presentation.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/budget/exercices/init_create}.
 *
 * <p>Seule l'année est saisie : dates et état sont fixés par le serveur, et
 * l'auteur est lu dans le jeton.</p>
 *
 * @param annee année civile de l'exercice, par exemple 2027
 */
public record CreerExerciceRequest(
        @Schema(example = "2027")
        @NotNull(message = "Indiquez l'année de l'exercice.")
        Integer annee
) {
}
