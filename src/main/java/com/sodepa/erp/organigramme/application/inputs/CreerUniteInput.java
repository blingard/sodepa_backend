package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.TypeUniteOrganisationnelle;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CreerUniteInput(
        String code,
        String intitule,
        TypeUniteOrganisationnelle typeUnite,
        UUID typeStructureId,
        UUID parentId,
        String localisation,
        LocalDate dateCreation
) {}
