package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `GenerateAmortisationInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record GenerateAmortisationInput(
        @NotNull @Min(1900) int annee,
        @NotBlank String compteImmoCode
) {
}
