package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `RejeterInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record RejeterInput(
    String numeroEngagement,
    String motif,
    UUID utilisateurId
) {}
