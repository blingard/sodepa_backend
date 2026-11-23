package com.sodepa.erp.budgetaire.nomenclature.application.inputs;

import java.util.UUID;

/**
 * Demande de modification d'un élément : l'état voulu de ses champs modifiables.
 *
 * <p>Le code, le niveau et le parent ne se modifient pas (voir
 * {@code ElementProgrammatiqueEntity}). Un champ absent est un champ vidé.</p>
 *
 * @param id            élément à modifier
 * @param libelle       nouveau libellé
 * @param description   nouvelle description, ou {@code null} pour l'effacer
 * @param responsableId nouveau responsable, ou {@code null} pour le retirer (hors programme)
 */
public record ModifierElementInput(UUID id, String libelle, String description, UUID responsableId) {
}
