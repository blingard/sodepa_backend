package com.sodepa.erp.rh.application;

import com.sodepa.erp.workflow.application.WorkflowInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RhWorkflowEventListener {

    private final RhService rhService;

    @EventListener
    public void onWorkflowCompleted(WorkflowInstanceCompletedEvent event) {
        log.info("Événement de clôture workflow reçu: [{}] pour l'entité {}/{}",
                event.workflowCode(), event.entityName(), event.entityId());

        if ("WF-RH-RECRUTEMENT".equalsIgnoreCase(event.workflowCode())
                || "WF-DAF-07".equalsIgnoreCase(event.workflowCode())
                || "EMPLOYE".equalsIgnoreCase(event.entityName())) {
            try {
                UUID employeId = UUID.fromString(event.entityId());
                rhService.finaliserApprobationRecrutementDg(
                        employeId,
                        event.commentaireFinal(),
                        event.dernierActeurId()
                );
            } catch (Exception e) {
                log.error("Erreur lors de la finalisation RH suite à l'approbation du workflow {}", event.instanceId(), e);
            }
        }
    }
}
