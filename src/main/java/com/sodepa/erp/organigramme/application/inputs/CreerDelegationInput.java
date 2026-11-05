package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.TypeDelegation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record CreerDelegationInput(
        UUID delegantId,
        UUID delegataireId,
        TypeDelegation typeDelegation,
        BigDecimal plafondMontant,
        Instant dateDebut,
        Instant dateFin
) {}
