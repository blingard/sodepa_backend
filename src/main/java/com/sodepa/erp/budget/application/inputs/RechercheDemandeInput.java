package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

/**
 * Critères de recherche des demandes budgétaires départementales.
 *
 * <p>
 * Les trois critères sont facultatifs et se combinent. Sans pagination : une
 * campagne budgétaire se compte en dizaines de demandes par département, et
 * l'écran d'arbitrage les veut toutes sous les yeux.
 * </p>
 *
 * @param departementId département demandeur ; {@code null} pour tous
 * @param annee exercice visé ; {@code null} pour tous
 * @param statut état de la demande ; {@code null} pour tous
 */
public record RechercheDemandeInput(
    UUID departementId,
    Integer annee,
    String statut
) {
}
