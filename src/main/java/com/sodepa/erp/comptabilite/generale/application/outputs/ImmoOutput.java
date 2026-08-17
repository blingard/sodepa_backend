package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.StatutImmobilisation;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO complet d'une immobilisation.
 */
@Builder
public record ImmoOutput(
    UUID id,
    String code,
    String designation,
    BigDecimal valeurOrigine,
    LocalDate dateAcquisition,
    LocalDate dateMiseEnService,
    ModeAmortissement modeAmortissement,
    Integer dureeUtile,
    BigDecimal valeurResiduelle,
    StatutImmobilisation statut,
    BigDecimal amortissementCumule
) {
}
