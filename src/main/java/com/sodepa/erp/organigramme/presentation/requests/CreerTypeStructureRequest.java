package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CreerTypeStructureInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreerTypeStructureRequest(
        @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String libelle,
        String description,
        UUID typeParentId,
        Integer niveauHierarchique,
        Boolean actif,
        String couleur,
        String icone,
        String missionsTypes
) {
    public CreerTypeStructureInput toInput() {
        return CreerTypeStructureInput.builder()
                .code(code)
                .libelle(libelle)
                .description(description)
                .typeParentId(typeParentId)
                .niveauHierarchique(niveauHierarchique)
                .actif(actif != null ? actif : true)
                .couleur(couleur != null ? couleur : "#4f46e5")
                .icone(icone != null ? icone : "solar:buildings-2-line-duotone")
                .missionsTypes(missionsTypes)
                .build();
    }
}
