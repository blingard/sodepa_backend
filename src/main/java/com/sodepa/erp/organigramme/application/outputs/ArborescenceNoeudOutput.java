package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutUnite;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeUniteOrganisationnelle;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Composant applicatif `ArborescenceNoeudOutput` du système ERP SODEPA.
 */
@Builder
public record ArborescenceNoeudOutput(
        UUID id,
        String code,
        String intitule,
        TypeUniteOrganisationnelle typeUnite,
        UUID typeStructureId,
        String typeStructureLibelle,
        String typeStructureCode,
        String typeStructureCouleur,
        String typeStructureIcone,
        String materializedPath,
        String localisation,
        StatutUnite statut,
        int totalPostesAutorises,
        int totalPostesPourvus,
        String responsableNom,
        String responsablePoste,
        BigDecimal tauxMoyenObjectifs,
        int nbObjectifs,
        List<ArborescenceNoeudOutput> sousUnites
) {}
