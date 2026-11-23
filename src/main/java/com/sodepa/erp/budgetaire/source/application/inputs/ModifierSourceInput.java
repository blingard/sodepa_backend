package com.sodepa.erp.budgetaire.source.application.inputs;

import java.util.UUID;

/**
 * Demande de modification d'une source : l'état voulu du libellé et du
 * bailleur. Le code et le type ne changent pas.
 *
 * @param id       source visée
 * @param libelle  nouveau libellé
 * @param bailleur nouveau bailleur, ou {@code null} pour l'effacer (hors partenaire)
 */
public record ModifierSourceInput(UUID id, String libelle, String bailleur) {
}
