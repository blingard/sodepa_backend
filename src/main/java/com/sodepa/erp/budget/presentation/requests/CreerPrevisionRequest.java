package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreerPrevisionRequest(
    @NotNull LocalDate dateEcheance,
    @NotBlank String type,
    @NotBlank String source,
    @NotBlank String libelle,
    @NotNull @Positive BigDecimal montant
) {}
