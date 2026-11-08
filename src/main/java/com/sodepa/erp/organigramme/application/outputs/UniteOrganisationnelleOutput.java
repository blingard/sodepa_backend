package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutUnite;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeUniteOrganisationnelle;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `UniteOrganisationnelleOutput` du système ERP SODEPA.
 */
@Builder
public record UniteOrganisationnelleOutput(
        UUID id,
        String code,
        String intitule,
        TypeUniteOrganisationnelle typeUnite,
        UUID typeStructureId,
        String typeStructureLibelle,
        String typeStructureCode,
        String typeStructureCouleur,
        String typeStructureIcone,
        UUID parentId,
        String parentIntitule,
        String materializedPath,
        String localisation,
        StatutUnite statut,
        LocalDate dateCreation,
        LocalDate dateFermeture,
        int nbSousUnites,
        int nbPostes,
        BigDecimal tauxMoyenObjectifs,
        int nbObjectifs
) {}
