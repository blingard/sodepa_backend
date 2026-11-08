package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Composant applicatif `BudgetEngagementOutput` du système ERP SODEPA.
 */
public record BudgetEngagementOutput(
    UUID id,
    UUID budgetItemId,
    String numeroEngagement,
    String description,
    BigDecimal montant,
    LocalDateTime dateEngagement,
    String statut
) {}
