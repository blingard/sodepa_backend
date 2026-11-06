package com.sodepa.erp.rh.application;

import com.sodepa.erp.workflow.application.WorkflowInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Écouteur d'événements asynchrones du module Ressources Humaines (RH).
 * <p>
 * Réagit aux événements {@link WorkflowInstanceCompletedEvent} émis par le moteur de workflow universel
 * lorsque des circuits liés aux employés ou aux recrutements sont totalement validés (ex: signature DG).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RhWorkflowEventListener {

    /** Service applicatif pour les opérations de gestion des employés et carrières. */
    private final RhService rhService;

    /**
     * Intercepte l'achèvement avec succès d'un circuit de validation pour finaliser les dossiers RH.
     * <p>
     * Cas traités :
     * <ul>
     *   <li>{@code WF-RH-RECRUTEMENT} ou {@code WF-DAF-07} : déclenche la finalisation de l'approbation DG,
     *   génère le matricule et active l'employé dans le système.</li>
     * </ul>
     * </p>
     *
     * @param event Événement de complétion émis par le moteur de workflow universel.
     */
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
