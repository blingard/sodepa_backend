package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerTypeStructureInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
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
