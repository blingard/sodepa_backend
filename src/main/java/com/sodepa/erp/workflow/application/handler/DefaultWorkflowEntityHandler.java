package com.sodepa.erp.workflow.application.handler;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler par défaut (fallback) assurant la traçabilité des mutations
 * pour toute entité métier n'ayant pas de handler spécialisé dédié.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultWorkflowEntityHandler implements WorkflowEntityHandler {

    @Override
    public boolean supports(String entityName) {
        // Sert de fallback pour toute entité
        return true;
    }

    @Override
    public void onStepApproved(WorkflowTaskEntity task, Map<String, Object> modifiedFields) {
        log.info("[WORKFLOW-HANDLER-DEFAULT] Étape approuvée sur entité {}/{} | Champs enrichis: {}",
                task.getWorkflowInstance().getEntityName(),
                task.getWorkflowInstance().getEntityId(),
                modifiedFields != null ? modifiedFields.keySet() : "aucun");
    }

    @Override
    public void onWorkflowCompleted(WorkflowInstanceEntity instance) {
        log.info("[WORKFLOW-HANDLER-DEFAULT] Circuit complété avec succès pour entité {}/{}",
                instance.getEntityName(), instance.getEntityId());
    }

    @Override
    public void onWorkflowRejected(WorkflowInstanceEntity instance, String motif) {
        log.warn("[WORKFLOW-HANDLER-DEFAULT] Circuit rejeté pour entité {}/{} | Motif: {}",
                instance.getEntityName(), instance.getEntityId(), motif);
    }
}
