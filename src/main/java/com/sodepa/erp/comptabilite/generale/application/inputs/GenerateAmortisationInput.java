package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateAmortisationInput(
        @NotNull @Min(1900) int annee,
        @NotBlank String compteImmoCode
) {
}
