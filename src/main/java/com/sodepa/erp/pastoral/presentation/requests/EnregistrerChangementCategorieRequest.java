package com.sodepa.erp.pastoral.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerChangementCategorieRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerChangementCategorieRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément bovin associé.
     */
    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    /**
     * Code normalisé unique identifiant nouvelleCategorieCode dans le référentiel SODEPA.
     */
    @NotBlank(message = "La nouvelle catégorie est obligatoire")
    private String nouvelleCategorieCode;

    /**
     * Champ dateChangement : date changement utilisé pour le pilotage et le suivi dans EnregistrerChangementCategorieRequest.
     */
    private LocalDate dateChangement;

    /**
     * Champ justificatifRef : justificatif ref utilisé pour le pilotage et le suivi dans EnregistrerChangementCategorieRequest.
     */
    private String justificatifRef;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans EnregistrerChangementCategorieRequest.
     */
    private String observations;
}
