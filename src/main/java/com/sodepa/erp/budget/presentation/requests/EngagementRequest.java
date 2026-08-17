package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record EngagementRequest(
    @NotNull UUID planId,
    @NotBlank String compteCode,
    UUID sectionId,
    @NotBlank String numeroEngagement,
    @NotBlank String description,
    @NotNull @Positive BigDecimal montant,
    @NotNull UUID utilisateurId
) {}
