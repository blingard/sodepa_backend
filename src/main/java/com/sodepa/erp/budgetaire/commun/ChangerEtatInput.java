package com.sodepa.erp.budgetaire.commun;

import java.util.UUID;

/**
 * Demande de désactivation ou de réactivation d'un objet d'un référentiel budgétaire.
 *
 * @param id    objet visé
 * @param motif motif, obligatoire pour une désactivation
 */
public record ChangerEtatInput(UUID id, String motif) {
}
