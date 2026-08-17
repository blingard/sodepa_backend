package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreerPrevisionInput(
    LocalDate dateEcheance,
    String type,
    String source,
    String libelle,
    BigDecimal montant
) {}
