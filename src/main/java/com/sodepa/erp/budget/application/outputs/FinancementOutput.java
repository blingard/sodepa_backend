package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancementOutput(
    UUID id,
    UUID banqueId,
    String intitule,
    String type,
    BigDecimal capitalEmprunte,
    BigDecimal tauxNominal,
    LocalDate dateEffet,
    int dureeMois,
    String periodicite,
    String statut,
    List<EcheanceOutput> echeances
) {}
