package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CouvertureInput(
    String reference,
    String devise,
    BigDecimal montantDevise,
    BigDecimal coursGaranti,
    LocalDate dateEffet,
    LocalDate dateEcheance
) {}
