package com.sodepa.erp.comptabilite.generale.infrastructure.event;

import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.StatutImmobilisation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload sérialisé dans le système Maker-Checker pour les opérations sur les immobilisations.
 */
public record ImmoEventInput(
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
    String userId
) {
}
