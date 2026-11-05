package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposerReclassementRequest {

    @NotNull(message = "L'identifiant de l'employé est obligatoire")
    private UUID employeId;

    @NotBlank(message = "L'intitulé du diplôme / formation est obligatoire")
    private String intituleDiplomeFormation;

    @NotNull(message = "La durée de la formation en mois est obligatoire")
    private Integer dureeFormationMois;

    @NotNull(message = "La nouvelle catégorie est obligatoire")
    private Integer nouvelleCategorie;

    @NotBlank(message = "Le nouvel échelon est obligatoire")
    private String nouvelEchelon;

    private LocalDate dateEffet;

    private String decisionDgRef;
}
