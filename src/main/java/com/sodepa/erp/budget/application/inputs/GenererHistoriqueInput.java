package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

public record GenererHistoriqueInput(
    int anneeSource,
    int anneeCible,
    BigDecimal coeffVentes,
    BigDecimal coeffCharges,
    UUID departementId
) {}
