package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.TypeAffectation;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `AffecterCollaborateurInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record AffecterCollaborateurInput(
        UUID utilisateurId,
        UUID posteId,
        TypeAffectation typeAffectation,
        LocalDate dateDebut,
        LocalDate dateFin,
        String referenceDecision
) {}
