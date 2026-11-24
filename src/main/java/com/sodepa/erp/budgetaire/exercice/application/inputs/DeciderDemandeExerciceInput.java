package com.sodepa.erp.budgetaire.exercice.application.inputs;

import com.sodepa.erp.utils.MakerCheckerStatus;

import java.util.UUID;

/**
 * Décision d'un validateur sur une demande portant sur un exercice.
 *
 * @param requestId identifiant de la <strong>demande</strong>, pas de l'exercice
 * @param decision  {@code ACCEPTED} ou {@code REJECTED}
 * @param notes     commentaire du validateur ; obligatoire pour un rejet
 */
public record DeciderDemandeExerciceInput(UUID requestId, MakerCheckerStatus decision, String notes) {
}
