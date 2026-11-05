package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CloturerAffectationInput(
        UUID affectationId,
        LocalDate dateFin,
        String motifFin
) {}
