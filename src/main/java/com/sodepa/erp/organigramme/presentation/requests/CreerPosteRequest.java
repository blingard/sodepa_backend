package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CreerPosteInput;
import com.sodepa.erp.organigramme.infrastructure.entities.CategorieConvention;
import com.sodepa.erp.utils.Permissions;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerPosteRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerPosteRequest(
        @NotBlank @Size(max = 50) String codePoste,
        @NotBlank @Size(max = 150) String intitule,
        @NotNull UUID uniteOrganisationnelleId,
        UUID posteSuperieurId,
        Integer effectifAutorise,
        @NotNull CategorieConvention categorieConvention,
        String descriptionMission,
        Set<Permissions> permissions
) {
    public CreerPosteInput toInput() {
        return CreerPosteInput.builder()
                .codePoste(codePoste)
                .intitule(intitule)
                .uniteOrganisationnelleId(uniteOrganisationnelleId)
                .posteSuperieurId(posteSuperieurId)
                .effectifAutorise(effectifAutorise)
                .categorieConvention(categorieConvention)
                .descriptionMission(descriptionMission)
                .permissions(permissions)
                .build();
    }
}
