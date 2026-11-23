package com.sodepa.erp.budgetaire.nomenclature.application.inputs;

import java.util.UUID;

/**
 * Demande de désactivation ou de réactivation d'un élément.
 *
 * @param id    élément visé
 * @param motif motif, obligatoire pour une désactivation
 */
public record ChangerEtatElementInput(UUID id, String motif) {
}
