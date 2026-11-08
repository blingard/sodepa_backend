package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `DemanderRallongeStageRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemanderRallongeStageRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément stage associé.
     */
    @NotNull(message = "L'identifiant du stage est obligatoire")
    private UUID stageId;

    /**
     * Champ nouvelleDateFin : nouvelle date fin utilisé pour le pilotage et le suivi dans DemanderRallongeStageRequest.
     */
    @NotNull(message = "La nouvelle date de fin souhaitée est obligatoire")
    private LocalDate nouvelleDateFin;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    private String motif;
}
