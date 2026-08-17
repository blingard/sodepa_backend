package com.sodepa.erp.budget.presentation.requests;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record ValiderEtapeRequest(
    @NotBlank String numeroEngagement,
    @NotBlank String roleApprobateur,
    @NotNull UUID utilisateurId
) {}
