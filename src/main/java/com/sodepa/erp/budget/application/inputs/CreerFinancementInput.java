package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
