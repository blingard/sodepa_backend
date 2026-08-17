package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BfrReportOutput(
    LocalDate dateCalcul,
    BigDecimal creancesClientsAttendues,
    BigDecimal dettesFournisseursPrevues,
    BigDecimal bfrEstime
) {}
