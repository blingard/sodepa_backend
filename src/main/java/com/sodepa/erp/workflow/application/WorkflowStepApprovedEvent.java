package com.sodepa.erp.workflow.application;

import java.util.Map;
import java.util.UUID;

/**
 * Événement métier Spring publié lorsqu'une étape intermédiaire de workflow est approuvée.
 * <p>
 * Permet à de multiples modules métiers (ex: Achats, Budget, Comptabilité, RH)
 * d'intervenir de manière transversale lors du passage d'une étape spécifique
 * (ex: réservation budgétaire, visa technique, notification spécifique).
 * </p>
 *
 * @param instanceId      Identifiant unique de l'instance de workflow.
 * @param workflowCode    Code du circuit de validation.
 * @param entityName      Nom fonctionnel de l'entité métier ciblée.
 * @param entityId        Identifiant technique de l'entité métier.
 * @param etapeCode       Code de l'étape venant d'être validée.
 * @param taskId          Identifiant de la tâche validée.
 * @param acteurId        Identifiant de l'approbateur.
 * @param modifiedFields  Champs et attributs enrichis lors de la validation.
 */
public record WorkflowStepApprovedEvent(
        UUID instanceId,
        String workflowCode,
        String entityName,
        String entityId,
        String etapeCode,
        UUID taskId,
        UUID acteurId,
        Map<String, Object> modifiedFields
) {}
