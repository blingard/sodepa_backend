package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutObjectif;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ObjectifUniteOutput(
        UUID id,
        UUID uniteId,
        String uniteIntitule,
        String intitule,
        String description,
        String indicateur,
        String uniteMesure,
        BigDecimal valeurCible,
        BigDecimal valeurRealisee,
        BigDecimal tauxAtteinte,
        Integer anneeExercice,
        LocalDate dateEcheance,
        StatutObjectif statut,
        String responsableNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
