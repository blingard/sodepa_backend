package com.sodepa.erp.paie.presentation.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerPeriodePaieRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerPeriodePaieRequest {

    /**
     * Champ annee : annee utilisé pour le pilotage et le suivi dans CreerPeriodePaieRequest.
     */
    @NotNull(message = "L'année est obligatoire")
    @Min(value = 2020, message = "Année invalide")
    private Integer annee;

    /**
     * Champ mois : mois utilisé pour le pilotage et le suivi dans CreerPeriodePaieRequest.
     */
    @NotNull(message = "Le mois est obligatoire")
    @Min(value = 1, message = "Mois entre 1 et 12")
    @Max(value = 12, message = "Mois entre 1 et 12")
    private Integer mois;
}
