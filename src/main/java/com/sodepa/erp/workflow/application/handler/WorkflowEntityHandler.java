package com.sodepa.erp.workflow.application.handler;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;

import java.util.Map;

/**
 * Interface Strategy pour le traitement métier spécifique lié aux entités du workflow SODEPA.
 * <p>
 * Permet de découpler le moteur universel de workflow des entités métiers du système.
 * Chaque domaine applicatif (Achats, RH, Budget, Comptabilité, etc.) implémente cette interface
 * pour gérer les mutations d'étapes intermédiaires et la finalisation du circuit.
 * </p>
 */
public interface WorkflowEntityHandler {

    /**
     * Indique si ce handler prend en charge l'entité donnée (ex: "ACHAT", "ENGAGEMENT_BUDGETAIRE", "MISSION").
     *
     * @param entityName Nom technique de l'entité.
     * @return true si pris en charge, false sinon.
     */
    boolean supports(String entityName);

    /**
     * Appelé lorsqu'une étape de workflow est approuvée par un acteur.
     * Permet d'appliquer les ajustements ou champs modifiés sur l'entité métier.
     *
     * @param task           La tâche qui vient d'être approuvée.
     * @param modifiedFields Dictionnaire des champs modifiés ou enrichis.
     */
    void onStepApproved(WorkflowTaskEntity task, Map<String, Object> modifiedFields);

    /**
     * Appelé lorsque le workflow arrive à son terme avec succès (toutes les étapes approuvées).
     *
     * @param instance L'instance de workflow finalisée.
     */
    void onWorkflowCompleted(WorkflowInstanceEntity instance);

    /**
     * Appelé lorsque le workflow est rejeté définitivement.
     *
     * @param instance L'instance de workflow rejetée.
     * @param motif    Motif explicite du rejet.
     */
    void onWorkflowRejected(WorkflowInstanceEntity instance, String motif);
}
