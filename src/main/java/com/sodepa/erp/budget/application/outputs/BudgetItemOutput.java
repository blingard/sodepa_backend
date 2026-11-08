package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Composant applicatif `BudgetItemOutput` du système ERP SODEPA.
 */
public record BudgetItemOutput(
    UUID id,
    String compteCode,
    UUID sectionId,
    BigDecimal montantAnnuel,
    BigDecimal montantPlanned,
    BigDecimal montantEngage,
    BigDecimal montantReal
) {}
