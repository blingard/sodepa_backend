package com.sodepa.erp.organigramme.application.outputs;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TypeStructureOutput(
        UUID id,
        String code,
        String libelle,
        String description,
        UUID typeParentId,
        String typeParentLibelle,
        Integer niveauHierarchique,
        Boolean actif,
        String couleur,
        String icone,
        String missionsTypes,
        int nbUnitesRattachees,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
