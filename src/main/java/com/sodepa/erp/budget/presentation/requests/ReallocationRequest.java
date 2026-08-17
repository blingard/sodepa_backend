package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ReallocationRequest(
    @NotNull UUID sourceItemId,
    @NotNull UUID destItemId,
    @NotNull @Positive BigDecimal montant,
    @NotNull UUID responsableId,
    @NotBlank String raison
) {}
