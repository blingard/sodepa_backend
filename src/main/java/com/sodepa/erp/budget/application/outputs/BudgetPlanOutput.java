package com.sodepa.erp.budget.application.outputs;

import com.sodepa.erp.budget.infrastructure.entities.StatutBudget;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record BudgetPlanOutput(
    UUID id,
    int annee,
    String intitule,
    int version,
    StatutBudget statut,
    BigDecimal totalBudget,
    LocalDateTime creeLe,
    UUID creePar,
    LocalDateTime modifieLe,
    UUID modifiePar,
    List<BudgetItemOutput> items
) {}
