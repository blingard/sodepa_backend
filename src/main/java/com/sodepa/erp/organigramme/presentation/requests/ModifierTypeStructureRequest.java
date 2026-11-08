package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.ModifierTypeStructureInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ModifierTypeStructureRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record ModifierTypeStructureRequest(
        @NotBlank @Size(max = 100) String libelle,
        String description,
        UUID typeParentId,
        Integer niveauHierarchique,
        Boolean actif,
        String couleur,
        String icone,
        String missionsTypes
) {
    public ModifierTypeStructureInput toInput(UUID id) {
        return ModifierTypeStructureInput.builder()
                .id(id)
                .libelle(libelle)
                .description(description)
                .typeParentId(typeParentId)
                .niveauHierarchique(niveauHierarchique)
                .actif(actif)
                .couleur(couleur)
                .icone(icone)
                .missionsTypes(missionsTypes)
                .build();
    }
}
