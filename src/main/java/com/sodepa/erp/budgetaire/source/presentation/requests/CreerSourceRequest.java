package com.sodepa.erp.budgetaire.source.presentation.requests;

import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/budget/sources-financement/init_create}.
 *
 * @param code     code de la source
 * @param libelle  libellé
 * @param type     {@code RESSOURCES_PROPRES}, {@code SUBVENTION} ou {@code PARTENAIRE}
 * @param bailleur partenaire ou bailleur ; obligatoire pour un partenaire
 */
public record CreerSourceRequest(
        @Schema(example = "RP")
        @NotBlank(message = "Indiquez le code de la source.")
        @Size(max = 30, message = "Le code ne doit pas dépasser 30 caractères.")
        String code,
        @Schema(example = "Ressources propres")
        @NotBlank(message = "Indiquez le libellé de la source.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String libelle,
        @Schema(example = "RESSOURCES_PROPRES")
        @NotNull(message = "Indiquez le type de la source.")
        TypeSourceFinancement type,
        @Size(max = 255, message = "Le nom du bailleur ne doit pas dépasser 255 caractères.")
        String bailleur
) {
}
