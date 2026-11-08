package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CloturerAffectationInput;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CloturerAffectationRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CloturerAffectationRequest(
        LocalDate dateFin,
        String motifFin
) {
    public CloturerAffectationInput toInput(UUID affectationId) {
        return CloturerAffectationInput.builder()
                .affectationId(affectationId)
                .dateFin(dateFin)
                .motifFin(motifFin)
                .build();
    }
}
