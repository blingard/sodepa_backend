package com.sodepa.erp.budget.presentation.requests;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreerBudgetRequest(
    @Positive int annee,
    @NotBlank String intitule,
    @NotNull UUID utilisateurId
) {}
