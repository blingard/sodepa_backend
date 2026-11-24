package com.sodepa.erp.budgetaire.nature.presentation.requests;

import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/budget/natures/init_create}.
 *
 * @param code       code de la nature
 * @param libelle    libellé
 * @param section    {@code FONCTIONNEMENT}, {@code INVESTISSEMENT} ou {@code PRODUITS}
 * @param sens       {@code DEPENSE} ou {@code RECETTE}
 * @param compteCode code du compte SYSCOHADA, qui doit figurer au plan comptable
 */
public record CreerNatureRequest(
        @Schema(example = "VENTES-BOVINS")
        @NotBlank(message = "Indiquez le code de la nature.")
        @Size(max = 30, message = "Le code ne doit pas dépasser 30 caractères.")
        String code,
        @Schema(example = "Ventes de bovins")
        @NotBlank(message = "Indiquez le libellé de la nature.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String libelle,
        @Schema(example = "PRODUITS")
        @NotNull(message = "Indiquez la section.")
        SectionBudgetaire section,
        @Schema(example = "RECETTE")
        @NotNull(message = "Indiquez le sens.")
        SensBudgetaire sens,
        @Schema(example = "701")
        @NotBlank(message = "Indiquez le compte SYSCOHADA.")
        @Size(max = 20, message = "Le code du compte ne doit pas dépasser 20 caractères.")
        String compteCode
) {
}
