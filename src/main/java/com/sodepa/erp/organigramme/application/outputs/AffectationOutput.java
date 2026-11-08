package com.sodepa.erp.organigramme.application.outputs;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeAffectation;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `AffectationOutput` du système ERP SODEPA.
 */
@Builder
public record AffectationOutput(
        UUID id,
        UUID utilisateurId,
        String utilisateurNom,
        String utilisateurEmail,
        UUID posteId,
        String posteCode,
        String posteIntitule,
        UUID uniteId,
        String uniteIntitule,
        TypeAffectation typeAffectation,
        LocalDate dateDebut,
        LocalDate dateFin,
        String referenceDecision,
        String motifFin,
        StatutAffectation statut
) {}
