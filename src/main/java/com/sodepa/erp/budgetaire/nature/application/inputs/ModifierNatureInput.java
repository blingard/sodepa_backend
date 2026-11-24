package com.sodepa.erp.budgetaire.nature.application.inputs;

import java.util.UUID;

/**
 * Demande de modification d'une nature : seul le libellé change. Le code, la
 * section, le sens et le compte sont figés, parce que les lignes s'en servent.
 *
 * @param id      nature visée
 * @param libelle nouveau libellé
 */
public record ModifierNatureInput(UUID id, String libelle) {
}
