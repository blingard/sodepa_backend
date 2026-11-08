package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CreerUniteInput;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeUniteOrganisationnelle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerUniteRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerUniteRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String intitule,
        @NotNull TypeUniteOrganisationnelle typeUnite,
        UUID typeStructureId,
        UUID parentId,
        @NotBlank @Size(max = 150) String localisation,
        LocalDate dateCreation
) {
    public CreerUniteInput toInput() {
        return CreerUniteInput.builder()
                .code(code)
                .intitule(intitule)
                .typeUnite(typeUnite)
                .typeStructureId(typeStructureId)
                .parentId(parentId)
                .localisation(localisation)
                .dateCreation(LocalDate.now())
                .build();
    }
}
