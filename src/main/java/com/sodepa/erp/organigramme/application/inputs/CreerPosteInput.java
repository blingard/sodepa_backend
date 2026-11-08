package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.CategorieConvention;
import com.sodepa.erp.utils.Permissions;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerPosteInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record CreerPosteInput(
        String codePoste,
        String intitule,
        UUID uniteOrganisationnelleId,
        UUID posteSuperieurId,
        Integer effectifAutorise,
        CategorieConvention categorieConvention,
        String descriptionMission,
        Set<Permissions> permissions
) {}
