package com.sodepa.erp.budget.application.inputs;

import java.time.LocalDate;

/**
 * Fenêtre de dates, bornes incluses.
 *
 * @param debut premier jour retenu
 * @param fin dernier jour retenu
 */
public record PeriodeInput(LocalDate debut, LocalDate fin) {
}
