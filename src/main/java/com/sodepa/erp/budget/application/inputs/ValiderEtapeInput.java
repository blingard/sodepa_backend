package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ValiderEtapeInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record ValiderEtapeInput(
    String numeroEngagement,
    String roleApprobateur,
    UUID utilisateurId
) {}
