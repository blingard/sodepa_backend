package com.sodepa.erp.organigramme.application.inputs;

import com.sodepa.erp.organigramme.infrastructure.entities.TypeAffectation;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record AffecterCollaborateurInput(
        UUID utilisateurId,
        UUID posteId,
        TypeAffectation typeAffectation,
        LocalDate dateDebut,
        LocalDate dateFin,
        String referenceDecision
) {}
