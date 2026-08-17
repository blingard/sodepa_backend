package com.sodepa.erp.budget.presentation.requests;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record RejeterRequest(
    @NotBlank String numeroEngagement,
    @NotBlank String motif,
    @NotNull UUID utilisateurId
) {}
