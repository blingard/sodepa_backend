package com.sodepa.erp.workflow.application;

import java.util.UUID;

/**
 * Événement métier Spring publié lorsqu'une demande de révision / modification est émise.
 * <p>
 * Permet aux modules concernés de suspendre temporairement les traitements associés
 * en attente du retour de l'initiateur.
 * </p>
 *
 * @param instanceId     Identifiant unique de l'instance de workflow suspendue.
 * @param workflowCode   Code du circuit de validation.
 * @param entityName     Nom fonctionnel de l'entité métier ciblée.
 * @param entityId       Identifiant technique de l'entité métier.
 * @param etapeCode      Code de l'étape demandant révision.
 * @param acteurId       Identifiant du validateur demandant la révision.
 * @param motifRevision  Motif explicite de la demande de révision.
 */
public record WorkflowRevisionRequestedEvent(
        UUID instanceId,
        String workflowCode,
        String entityName,
        String entityId,
        String etapeCode,
        UUID acteurId,
        String motifRevision
) {}
