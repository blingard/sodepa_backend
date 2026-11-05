package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.CategorieConvention;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutPoste;
import com.sodepa.erp.utils.Permissions;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record PosteOutput(
        UUID id,
        String codePoste,
        String intitule,
        UUID uniteId,
        String uniteIntitule,
        UUID posteSuperieurId,
        String posteSuperieurIntitule,
        Integer effectifAutorise,
        Integer effectifActuel,
        CategorieConvention categorieConvention,
        StatutPoste statut,
        String descriptionMission,
        String titulaireActuelNom,
        UUID titulaireActuelId,
        Set<Permissions> permissions
) {}
