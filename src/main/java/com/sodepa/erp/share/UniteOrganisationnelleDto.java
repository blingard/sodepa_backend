package com.sodepa.erp.share;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutUnite;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeUniteOrganisationnelle;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UniteOrganisationnelleDto(UUID id, String code, String intitule, TypeUniteOrganisationnelle typeUnite,
                                        String materializedPath, String localisation, StatutUnite statut,
                                        LocalDate dateCreation, LocalDate dateFermeture) implements Serializable {
    public static UniteOrganisationnelleDto fromEntity(UniteOrganisationnelleEntity uniteOrganisationnelle) {
        return new UniteOrganisationnelleDto(uniteOrganisationnelle.getId(), uniteOrganisationnelle.getCode(),
                uniteOrganisationnelle.getIntitule(), uniteOrganisationnelle.getTypeUnite(),
                uniteOrganisationnelle.getMaterializedPath(), uniteOrganisationnelle.getLocalisation(),
                uniteOrganisationnelle.getStatut(), uniteOrganisationnelle.getDateCreation(),
                uniteOrganisationnelle.getDateFermeture());
    }
}