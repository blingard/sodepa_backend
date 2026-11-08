package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerBudgetPlanInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerBudgetPlanInput(
    int annee,
    String intitule,
    UUID utilisateurId
) {}
