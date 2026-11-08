package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutObjectif;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerObjectifInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record CreerObjectifInput(
        UUID uniteId,
        String intitule,
        String description,
        String indicateur,
        String uniteMesure,
        BigDecimal valeurCible,
        BigDecimal valeurRealisee,
        Integer anneeExercice,
        LocalDate dateEcheance,
        StatutObjectif statut,
        String responsableNom
) {}
