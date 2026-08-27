package com.sodepa.erp.budget.application.inputs;

/**
 * Critères de recherche du portefeuille de couvertures de change.
 *
 * @param devise devise cible ; {@code null} pour toutes
 * @param statut état du contrat ; {@code null} pour tous
 */
public record RechercheCouvertureInput(String devise, String statut) {
}
