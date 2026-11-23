package com.sodepa.erp.workflow.application.handler;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.budget.infrastructure.entities.BudgetPlanEntity;
import com.sodepa.erp.budget.infrastructure.repo.BudgetPlanRepository;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Handler workflow dédié aux plans budgétaires ({@code BUDGET_PLAN}).
 * <p>
 * Zéro écriture anticipée ni corruption lors des étapes intermédiaires ou d'un rejet ;
 * matérialisation finale en base PostgreSQL uniquement à la clôture {@code APPROUVE}.
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class BudgetPlanWorkflowEntityHandler implements WorkflowEntityHandler {

    private final BudgetPlanRepository budgetPlanRepository;

    @Autowired
    @Lazy
    private BudgetPlanAdapter budgetPlanAdapter;

    private static final String ENTITY_BUDGET_PLAN = "BUDGET_PLAN";
    private static final String ENTITY_CLASS = BudgetPlanEntity.class.getName();
    private static final String ENTITY_SIMPLE = BudgetPlanEntity.class.getSimpleName();

    @Override
    public boolean supports(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            return false;
        }
        String clean = entityName.trim();
        return clean.equalsIgnoreCase(ENTITY_BUDGET_PLAN)
                || clean.equalsIgnoreCase(ENTITY_SIMPLE)
                || clean.equals(ENTITY_CLASS);
    }

    @Override
    @Transactional
    public void onStepApproved(WorkflowTaskEntity task, Map<String, Object> modifiedFields) {
        if (task == null || task.getWorkflowInstance() == null) {
            return;
        }
        String codeEtape = task.getEtapeDefinition() != null ? task.getEtapeDefinition().getCodeEtape() : "N/A";
        log.debug("[BUDGET-WORKFLOW] Étape {} validée sur instance {} ({}) — état historisé dans ClickHouse avant clôture.",
                codeEtape, task.getWorkflowInstance().getId(), task.getWorkflowInstance().getEntityId());
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance) {
        onWorkflowCompleted(instance, null);
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance, Map<String, Object> finalData) {
        if (instance == null) {
            return;
        }
        UUID planId = parseUuid(instance.getEntityId());
        if (planId == null) {
            return;
        }
        if (budgetPlanRepository.findById(planId).isEmpty() && finalData != null && !finalData.isEmpty()) {
            budgetPlanAdapter.finaliserBudgetPlanDepuisWorkflow(planId, finalData);
        }
    }

    @Override
    @Transactional
    public void onWorkflowRejected(WorkflowInstanceEntity instance, String motif) {
        if (instance == null) {
            return;
        }
        log.warn("[BUDGET-WORKFLOW] Instance BUDGET_PLAN {} ({}) REJETEE sans matérialisation PostgreSQL. Motif: {}",
                instance.getId(), instance.getEntityId(), motif);
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
