package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutDelegation;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeDelegation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record DelegationOutput(
        UUID id,
        UUID delegantId,
        String delegantNom,
        UUID delegataireId,
        String delegataireNom,
        TypeDelegation typeDelegation,
        BigDecimal plafondMontant,
        Instant dateDebut,
        Instant dateFin,
        StatutDelegation statut
) {}
