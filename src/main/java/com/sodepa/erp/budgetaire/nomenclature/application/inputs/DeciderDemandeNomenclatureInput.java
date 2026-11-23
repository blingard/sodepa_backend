package com.sodepa.erp.budgetaire.nomenclature.application.inputs;

import com.sodepa.erp.utils.MakerCheckerStatus;

import java.util.UUID;

/**
 * Décision d'un validateur sur une demande portant sur la nomenclature.
 *
 * @param requestId identifiant de la <strong>demande</strong>, pas de l'élément
 * @param decision  {@code ACCEPTED} ou {@code REJECTED}
 * @param notes     commentaire du validateur ; obligatoire pour un rejet
 */
public record DeciderDemandeNomenclatureInput(UUID requestId, MakerCheckerStatus decision, String notes) {
}
