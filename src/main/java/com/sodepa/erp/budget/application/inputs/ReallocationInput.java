package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ReallocationInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record ReallocationInput(
    UUID sourceItemId,
    UUID destItemId,
    BigDecimal montant,
    UUID responsableId,
    String raison
) {}
