package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CouvertureInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CouvertureInput(
    String reference,
    String devise,
    BigDecimal montantDevise,
    BigDecimal coursGaranti,
    LocalDate dateEffet,
    LocalDate dateEcheance
) {}
