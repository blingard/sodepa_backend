package com.sodepa.erp.budgetaire.commun;

import com.sodepa.erp.utils.MakerCheckerStatus;

import java.util.UUID;

/**
 * Décision d'un validateur sur une demande portant sur un référentiel budgétaire.
 *
 * @param requestId identifiant de la <strong>demande</strong>, pas de l'objet
 * @param decision  {@code ACCEPTED} ou {@code REJECTED}
 * @param notes     commentaire du validateur ; obligatoire pour un rejet
 */
public record DeciderDemandeInput(UUID requestId, MakerCheckerStatus decision, String notes) {
}
