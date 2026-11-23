package com.sodepa.erp.budgetaire.structure.application.inputs;

import com.sodepa.erp.utils.MakerCheckerStatus;

import java.util.UUID;

/**
 * Décision d'un validateur sur une demande portant sur une structure.
 *
 * @param requestId identifiant de la <strong>demande</strong>, pas de la structure
 * @param decision  {@code ACCEPTED} ou {@code REJECTED}
 * @param notes     commentaire du validateur ; obligatoire pour un rejet
 */
public record DeciderDemandeStructureInput(UUID requestId, MakerCheckerStatus decision, String notes) {
}
