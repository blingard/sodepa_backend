package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreerHorsBilanInput(
    String type,
    String intitule,
    UUID tiersId,
    BigDecimal montant,
    LocalDate dateEffet,
    LocalDate dateEcheance
) {}
