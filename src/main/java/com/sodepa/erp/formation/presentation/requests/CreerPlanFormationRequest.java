package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerPlanFormationRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerPlanFormationRequest {

    /**
     * Champ anneeExercice : annee exercice utilisé pour le pilotage et le suivi dans CreerPlanFormationRequest.
     */
    @NotNull(message = "L'année d'exercice est obligatoire")
    private Integer anneeExercice;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @NotBlank(message = "L'intitulé du plan est obligatoire")
    private String intitule;

    /**
     * Champ budgetTotalPrevu : budget total prevu utilisé pour le pilotage et le suivi dans CreerPlanFormationRequest.
     */
    private BigDecimal budgetTotalPrevu;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans CreerPlanFormationRequest.
     */
    private String observations;
}
