package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.StatutImmobilisation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Modification d'une immobilisation, soumise au circuit maker-checker.
 *
 * <p>
 * Reprend tous les champs de la création, plus l'identifiant du bien visé et
 * son statut : c'est par là qu'une immobilisation est cédée ou mise au rebut.
 * L'amortissement cumulé n'y figure pas — il est calculé par les dotations, et
 * le corriger à la main romprait le lien avec les écritures passées.
 * </p>
 */
public record UpdateImmoInput(
    @NotNull UUID id,
    @NotBlank String code,
    @NotBlank String designation,
    @NotNull BigDecimal valeurOrigine,
    @NotNull LocalDate dateAcquisition,
    @NotNull LocalDate dateMiseEnService,
    @NotNull ModeAmortissement modeAmortissement,
    @NotNull Integer dureeUtile,
    BigDecimal valeurResiduelle,
    @NotNull StatutImmobilisation statut
) {
}
