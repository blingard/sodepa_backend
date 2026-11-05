package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CloturerAffectationInput;

import java.time.LocalDate;
import java.util.UUID;

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
