package com.sodepa.erp.budgetaire.structure.application.inputs;

import java.util.UUID;

/**
 * Demande de changement de l'ordonnateur ou du service d'engagement.
 * L'unité d'une structure ne change pas.
 *
 * @param id                       structure visée
 * @param ordonnateurPosteId       nouveau poste de l'ordonnateur
 * @param serviceEngagementUniteId nouvelle unité du service d'engagement
 */
public record ModifierStructureInput(UUID id, UUID ordonnateurPosteId, UUID serviceEngagementUniteId) {
}
