package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetItemOutput(
    UUID id,
    String compteCode,
    UUID sectionId,
    BigDecimal montantAnnuel,
    BigDecimal montantPlanned,
    BigDecimal montantEngage,
    BigDecimal montantReal
) {}
