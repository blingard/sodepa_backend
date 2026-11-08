package com.sodepa.erp.workflow.application;

import java.util.UUID;

/**
 * Événement métier Spring publié lorsqu'une instance de workflow est rejetée par un validateur obligatoire.
 *
 * @param instanceId        Identifiant unique de l'instance de workflow rejetée.
 * @param workflowCode      Code du circuit de validation.
 * @param entityName        Nom fonctionnel de l'entité métier ciblée.
 * @param entityId          Identifiant technique de l'entité métier.
 * @param etapeRejetCode    Code de l'étape où le rejet a eu lieu.
 * @param acteurRejetId     Identifiant de l'approbateur ayant rejeté la demandée.
 * @param motifRejet        Motif du rejet.
 */
public record WorkflowInstanceRejectedEvent(
        UUID instanceId,
        String workflowCode,
        String entityName,
        String entityId,
        String etapeRejetCode,
        UUID acteurRejetId,
        String motifRejet
) {}
