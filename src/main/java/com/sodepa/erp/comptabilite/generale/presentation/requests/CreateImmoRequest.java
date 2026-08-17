package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.ModeAmortissement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête d'enregistrement d'une immobilisation.
 */
public record CreateImmoRequest(
    @NotBlank String code,
    @NotBlank String designation,
    @NotNull @Positive BigDecimal valeurOrigine,
    @NotNull LocalDate dateAcquisition,
    @NotNull LocalDate dateMiseEnService,
    @NotNull ModeAmortissement modeAmortissement,
    @NotNull @Positive Integer dureeUtile,
    BigDecimal valeurResiduelle
) {
}
