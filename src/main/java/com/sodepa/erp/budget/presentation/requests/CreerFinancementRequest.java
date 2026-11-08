package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerFinancementRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerFinancementRequest(
    @NotNull UUID banqueId,
    @NotBlank String intitule,
    @NotBlank String type, // PRET, LEASING, OBLIGATION
    @NotNull @Positive BigDecimal capital,
    @NotNull @Positive BigDecimal tauxNominal,
    @NotNull LocalDate dateEffet,
    @Positive int dureeMois,
    @NotBlank String periodicite, // MENSUELLE, TRIMESTRIELLE, ANNUELLE
    @NotNull UUID utilisateurId
) {}
