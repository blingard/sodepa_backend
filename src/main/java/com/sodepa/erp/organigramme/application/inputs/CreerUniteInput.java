package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.TypeUniteOrganisationnelle;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerUniteInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
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
