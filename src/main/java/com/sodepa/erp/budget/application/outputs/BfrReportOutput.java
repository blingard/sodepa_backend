package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `BfrReportOutput`.
 */
public record BfrReportOutput(
    LocalDate dateCalcul,
    BigDecimal creancesClientsAttendues,
    BigDecimal dettesFournisseursPrevues,
    BigDecimal bfrEstime
) {}
