package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AjouterItemRequest(
    @NotBlank String compteCode,
    UUID sectionId,
    @NotNull @Positive BigDecimal montant
) {}
