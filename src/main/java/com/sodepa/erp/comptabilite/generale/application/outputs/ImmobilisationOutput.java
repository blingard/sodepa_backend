package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.StatutImmobilisation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.comptabilite.generale.infrastructure.entities.ImmobilisationEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImmobilisationOutput(@NotNull UUID id, @NotNull @NotEmpty @NotBlank String code,
                                   @NotNull @NotEmpty @NotBlank String designation,
                                   @NotNull @PositiveOrZero BigDecimal valeurOrigine, LocalDate dateAcquisition,
                                   LocalDate dateMiseEnService, ModeAmortissement modeAmortissement, Integer dureeUtile,
                                   BigDecimal valeurResiduelle, StatutImmobilisation statut,
                                   BigDecimal amortissementCumule) implements Serializable {
}