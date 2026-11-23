package com.sodepa.erp.budgetaire.nature.application.inputs;

import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;

/**
 * Demande de création d'une nature.
 *
 * @param code       code de la nature, mis en majuscules par le serveur
 * @param libelle    libellé (« Ventes de bovins »)
 * @param section    fonctionnement, investissement ou produits
 * @param sens       dépense ou recette
 * @param compteCode code du compte SYSCOHADA (« 701 »), qui doit exister au plan comptable
 */
public record CreerNatureInput(String code, String libelle, SectionBudgetaire section, SensBudgetaire sens,
                               String compteCode) {
}
