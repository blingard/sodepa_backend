package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutVersionOrganigramme;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record OrganigrammeVersionOutput(
        UUID id,
        String numeroVersion,
        String libelle,
        String description,
        LocalDate dateEffet,
        StatutVersionOrganigramme statut,
        UUID approuvePar,
        Instant dateApprobation
) {}
