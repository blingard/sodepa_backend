package com.sodepa.erp.share;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sodepa.erp.organigramme.infrastructure.entities.CategorieConvention;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutPoste;
import com.sodepa.erp.utils.Permissions;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PosteDto(UUID id, String codePoste, String intitule, UniteOrganisationnelleDto uniteOrganisationnelle,
                       Integer effectifAutorise, Integer effectifActuel, CategorieConvention categorieConvention,
                       StatutPoste statut, String descriptionMission,
                       Set<Permissions> permissions) implements Serializable {

    public static PosteDto fromEntity(PosteEntity poste) {
        return new PosteDto(poste.getId(), poste.getCodePoste(), poste.getIntitule(),
                UniteOrganisationnelleDto.fromEntity(poste.getUniteOrganisationnelle()),
                poste.getEffectifAutorise(), poste.getEffectifActuel(), poste.getCategorieConvention(),
                poste.getStatut(), poste.getDescriptionMission(), poste.getPermissions());
    }
}