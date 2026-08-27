package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.StatutImmobilisation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de modification d'une immobilisation.
 *
 * <p>
 * Jumelle de {@link CreateImmoRequest}, avec le statut en plus : c'est la seule
 * voie pour céder un bien ou le mettre au rebut. L'identifiant vient du chemin.
 * </p>
 */
public record UpdateImmoRequest(
    @NotBlank String code,
    @NotBlank String designation,
    @NotNull @Positive BigDecimal valeurOrigine,
    @NotNull LocalDate dateAcquisition,
    @NotNull LocalDate dateMiseEnService,
    @NotNull ModeAmortissement modeAmortissement,
    @NotNull @Positive Integer dureeUtile,
    BigDecimal valeurResiduelle,
    @NotNull StatutImmobilisation statut
) {
}
