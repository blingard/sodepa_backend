package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CreerTypeStructureInput(
        String code,
        String libelle,
        String description,
        UUID typeParentId,
        Integer niveauHierarchique,
        Boolean actif,
        String couleur,
        String icone,
        String missionsTypes
) {}
