package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenererHistoriqueRequest(
    @Positive int anneeSource,
    @Positive int anneeCible,
    @NotNull @Positive BigDecimal coeffVentes,
    @NotNull @Positive BigDecimal coeffCharges,
    @NotNull UUID departementId
) {}
