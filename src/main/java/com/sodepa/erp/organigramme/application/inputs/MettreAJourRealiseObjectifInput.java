package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record MettreAJourRealiseObjectifInput(
        UUID id,
        BigDecimal valeurRealisee
) {}
