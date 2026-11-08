package com.sodepa.erp.organigramme.application.outputs;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Composant applicatif `ChaineHierarchiqueOutput` du système ERP SODEPA.
 */
@Builder
public record ChaineHierarchiqueOutput(
        UUID collaborateurId,
        String collaborateurNom,
        String posteIntitule,
        String uniteIntitule,
        UUID managerDirectId,
        String managerDirectNom,
        String managerDirectPoste,
        boolean estEnDelegation,
        UUID delegataireActuelId,
        String delegataireActuelNom,
        List<EchelonHierarchiqueOutput> ligneHierarchique
) {
    @Builder
    public record EchelonHierarchiqueOutput(
            int niveau,
            UUID utilisateurId,
            String nom,
            String email,
            String posteCode,
            String posteIntitule,
            String uniteCode,
            String uniteIntitule
    ) {}
}
