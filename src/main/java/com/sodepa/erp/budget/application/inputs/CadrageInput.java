package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CadrageInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CadrageInput(
    int annee,
    String comptePrefix,
    BigDecimal coefficient,
    UUID responsableId
) {}
