package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de génération de dotations aux amortissements.
 */
public record GenerateAmortisationRequest(
    @NotNull @Min(1900) Integer annee,
    @NotBlank String compteImmoCode
) {
}
