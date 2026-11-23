package com.sodepa.erp.budgetaire.structure.application.inputs;

import java.util.UUID;

/**
 * Demande de déclaration d'une structure budgétaire.
 *
 * @param uniteId                  unité de l'organigramme à déclarer
 * @param ordonnateurPosteId       poste de l'ordonnateur, rattaché à cette unité
 * @param serviceEngagementUniteId unité du service d'engagement, dans son sous-arbre
 */
public record CreerStructureInput(UUID uniteId, UUID ordonnateurPosteId, UUID serviceEngagementUniteId) {
}
