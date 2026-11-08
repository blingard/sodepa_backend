package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerFinancementInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerFinancementInput(
    UUID banqueId,
    String intitule,
    String type,
    BigDecimal capital,
    BigDecimal tauxNominal,
    LocalDate dateEffet,
    int dureeMois,
    String periodicite,
    UUID utilisateurId
) {}
