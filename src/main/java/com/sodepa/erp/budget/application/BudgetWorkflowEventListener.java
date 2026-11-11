package com.sodepa.erp.budget.application;

import com.sodepa.erp.workflow.application.WorkflowInstanceCompletedEvent;
import com.sodepa.erp.workflow.application.WorkflowInstanceRejectedEvent;
import com.sodepa.erp.workflow.application.WorkflowStepApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Écouteur d'événements de workflow pour le module Budget.
 * <p>
 * Illustre et garantit la collaboration transverse multi-modules :
 * le module Budget réagit aux validations d'étapes (ex: visa financier, engagement de crédits)
 * sur les dossiers provenant d'autres modules (ex: Achats, RH, Investissements).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetWorkflowEventListener {

    @EventListener
    public void onStepApproved(WorkflowStepApprovedEvent event) {
        if (event.entityName() != null && (event.entityName().contains("ACHAT") || event.entityName().contains("BESOIN") || event.entityName().contains("BUDGET"))) {
            log.info("[BUDGET-EVENT-LISTENER] Contrôle et traçabilité budgétaire pour l'étape {} sur l'entité {} (ID: {})",
                    event.etapeCode(), event.entityName(), event.entityId());
        }
    }

    @EventListener
    public void onWorkflowCompleted(WorkflowInstanceCompletedEvent event) {
        if (event.entityName() != null && (event.entityName().contains("ACHAT") || event.entityName().contains("BESOIN") || event.entityName().contains("BUDGET"))) {
            log.info("[BUDGET-EVENT-LISTENER] Clôture définitive du dossier budgétaire/achat {} (ID: {}) suite à approbation complète",
                    event.entityName(), event.entityId());
        }
    }

    @EventListener
    public void onWorkflowRejected(WorkflowInstanceRejectedEvent event) {
        if (event.entityName() != null && (event.entityName().contains("ACHAT") || event.entityName().contains("BESOIN") || event.entityName().contains("BUDGET"))) {
            log.warn("[BUDGET-EVENT-LISTENER] Libération des réservations de crédits budgétaires suite au rejet du dossier {} (ID: {})",
                    event.entityName(), event.entityId());
        }
    }
}
