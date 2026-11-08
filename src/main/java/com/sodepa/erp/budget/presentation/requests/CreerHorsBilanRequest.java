package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerHorsBilanRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerHorsBilanRequest(
    @NotBlank String type,
    @NotBlank String intitule,
    @NotNull UUID tiersId,
    @NotNull @Positive BigDecimal montant,
    @NotNull LocalDate dateEffet,
    @NotNull LocalDate dateEcheance
) {}
