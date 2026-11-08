package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ProposerReclassementRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposerReclassementRequest {

    /**
     * Identifiant unique de l'employé concerné dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "L'identifiant de l'employé est obligatoire")
    private UUID employeId;

    /**
     * Champ intituleDiplomeFormation : intitule diplome formation utilisé pour le pilotage et le suivi dans ProposerReclassementRequest.
     */
    @NotBlank(message = "L'intitulé du diplôme / formation est obligatoire")
    private String intituleDiplomeFormation;

    /**
     * Champ dureeFormationMois : duree formation mois utilisé pour le pilotage et le suivi dans ProposerReclassementRequest.
     */
    @NotNull(message = "La durée de la formation en mois est obligatoire")
    private Integer dureeFormationMois;

    /**
     * Champ nouvelleCategorie : nouvelle categorie utilisé pour le pilotage et le suivi dans ProposerReclassementRequest.
     */
    @NotNull(message = "La nouvelle catégorie est obligatoire")
    private Integer nouvelleCategorie;

    /**
     * Champ nouvelEchelon : nouvel echelon utilisé pour le pilotage et le suivi dans ProposerReclassementRequest.
     */
    @NotBlank(message = "Le nouvel échelon est obligatoire")
    private String nouvelEchelon;

    /**
     * Champ dateEffet : date effet utilisé pour le pilotage et le suivi dans ProposerReclassementRequest.
     */
    private LocalDate dateEffet;

    /**
     * Champ decisionDgRef : decision dg ref utilisé pour le pilotage et le suivi dans ProposerReclassementRequest.
     */
    private String decisionDgRef;
}
