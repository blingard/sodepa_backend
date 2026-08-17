package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CouvertureRequest(
    @NotBlank String reference,
    @NotBlank String devise,
    @NotNull @Positive BigDecimal montantDevise,
    @NotNull @Positive BigDecimal coursGaranti,
    @NotNull LocalDate dateEffet,
    @NotNull LocalDate dateEcheance
) {}
