package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EngagementHorsBilanOutput(
    UUID id,
    String type,
    String intitule,
    UUID tiersId,
    String tiersNom,
    BigDecimal montant,
    LocalDate dateEffet,
    LocalDate dateEcheance,
    String statut
) {}
