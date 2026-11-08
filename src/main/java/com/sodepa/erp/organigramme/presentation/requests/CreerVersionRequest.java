package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CreerVersionOrganigrammeInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerVersionRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerVersionRequest(
        @NotBlank @Size(max = 50) String numeroVersion,
        @NotBlank @Size(max = 150) String libelle,
        String description,
        @NotNull LocalDate dateEffet
) {
    public CreerVersionOrganigrammeInput toInput() {
        return CreerVersionOrganigrammeInput.builder()
                .numeroVersion(numeroVersion)
                .libelle(libelle)
                .description(description)
                .dateEffet(dateEffet)
                .build();
    }
}
