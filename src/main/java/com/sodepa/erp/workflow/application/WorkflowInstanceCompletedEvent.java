package com.sodepa.erp.workflow.application;

import java.util.UUID;

/**
 * Événement publié lorsqu'une instance de workflow est totalement approuvée
 * ou clôturée au terme de toutes ses étapes.
 */
public record WorkflowInstanceCompletedEvent(
        UUID instanceId,
        String workflowCode,
        String entityName,
        String entityId,
        String derniereEtapeCode,
        UUID dernierActeurId,
        String commentaireFinal
) {}
