package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

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
