package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContratCouvertureOutput(
    UUID id,
    String reference,
    String deviseCible,
    BigDecimal montantDevise,
    BigDecimal coursGaranti,
    LocalDate dateEffet,
    LocalDate dateEcheance,
    String statut
) {}
