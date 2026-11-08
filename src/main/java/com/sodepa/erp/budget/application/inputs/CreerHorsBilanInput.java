package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerHorsBilanInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerHorsBilanInput(
    String type,
    String intitule,
    UUID tiersId,
    BigDecimal montant,
    LocalDate dateEffet,
    LocalDate dateEcheance
) {}
