package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.ModeAmortissement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreateImmoInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreateImmoInput(
    @NotBlank String code,
    @NotBlank String designation,
    @NotNull BigDecimal valeurOrigine,
    @NotNull LocalDate dateAcquisition,
    @NotNull LocalDate dateMiseEnService,
    @NotNull ModeAmortissement modeAmortissement,
    @NotNull Integer dureeUtile,
    BigDecimal valeurResiduelle
) {
}
