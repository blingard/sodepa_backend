package com.sodepa.erp.formation.presentation.responses;

import com.sodepa.erp.formation.infrastructure.entities.FormationReclassementEntity;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `FormationReclassementResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationReclassementResponse {

    private UUID id;
    private UUID employeId;
    private String employeMatricule;
    private String employeNom;
    private String intituleDiplomeFormation;
    private Integer dureeFormationMois;
    private Integer ancienneteAnnees;
    private Integer ancienneCategorie;
    private String ancienEchelon;
    private Integer nouvelleCategorie;
    private String nouvelEchelon;
    private String decisionDgRef;
    private LocalDate dateEffet;
    private boolean valideDg;

    public static FormationReclassementResponse fromEntity(FormationReclassementEntity entity) {
        if (entity == null) return null;
        return FormationReclassementResponse.builder()
                .id(entity.getId())
                .employeId(entity.getEmploye() != null ? entity.getEmploye().getId() : null)
                .employeMatricule(entity.getEmploye() != null ? entity.getEmploye().getMatricule() : null)
                .employeNom(entity.getEmploye() != null ? entity.getEmploye().getNom() + " " + (entity.getEmploye().getPrenom() != null ? entity.getEmploye().getPrenom() : "") : null)
                .intituleDiplomeFormation(entity.getIntituleDiplomeFormation())
                .dureeFormationMois(entity.getDureeFormationMois())
                .ancienneteAnnees(entity.getAncienneteAnnees())
                .ancienneCategorie(entity.getAncienneCategorie())
                .ancienEchelon(entity.getAncienEchelon())
                .nouvelleCategorie(entity.getNouvelleCategorie())
                .nouvelEchelon(entity.getNouvelEchelon())
                .decisionDgRef(entity.getDecisionDgRef())
                .dateEffet(entity.getDateEffet())
                .valideDg(entity.isValideDg())
                .build();
    }
}
