package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ModifierTypeStructureInput(
        UUID id,
        String libelle,
        String description,
        UUID typeParentId,
        Integer niveauHierarchique,
        Boolean actif,
        String couleur,
        String icone,
        String missionsTypes
) {}
