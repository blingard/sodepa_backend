package com.sodepa.erp.budgetaire.structure.application.inputs;

import java.util.UUID;

/**
 * Demande de désactivation ou de réactivation d'une structure.
 *
 * @param id    structure visée
 * @param motif motif, obligatoire pour une désactivation
 */
public record ChangerEtatStructureInput(UUID id, String motif) {
}
