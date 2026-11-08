package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.AffecterCollaborateurInput;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeAffectation;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `AffecterCollaborateurRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record AffecterCollaborateurRequest(
        @NotNull UUID utilisateurId,
        @NotNull UUID posteId,
        @NotNull TypeAffectation typeAffectation,
        LocalDate dateDebut,
        LocalDate dateFin,
        String referenceDecision
) {
    public AffecterCollaborateurInput toInput() {
        return AffecterCollaborateurInput.builder()
                .utilisateurId(utilisateurId)
                .posteId(posteId)
                .typeAffectation(typeAffectation)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .referenceDecision(referenceDecision)
                .build();
    }
}
