package com.sodepa.erp.budget.application.outputs;

import com.sodepa.erp.budget.infrastructure.entities.StatutBudget;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Composant applicatif `BudgetPlanOutput` du système ERP SODEPA.
 */
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
    List<BudgetItemOutput> items,
    boolean enCoursDeModification,
    boolean workflowEnCours,
    UUID workflowInstanceId,
    String workflowStatut,
    String workflowOperationType,
    List<Map<String, Object>> piecesJointesWorkflow
) {
    public BudgetPlanOutput(
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
    ) {
        this(id, annee, intitule, version, statut, totalBudget, creeLe, creePar, modifieLe, modifiePar, items,
                false, false, null, null, null, Collections.emptyList());
    }
}
