package com.sodepa.erp.configuration.parametres.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `UpdateParametreRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Data
public class UpdateParametreRequest {

    /**
     * Champ valeur : valeur utilisé pour le pilotage et le suivi dans UpdateParametreRequest.
     */
    @NotBlank(message = "La nouvelle valeur est obligatoire")
    private String valeur;

    /**
     * Champ modifiePar : modifie par utilisé pour le pilotage et le suivi dans UpdateParametreRequest.
     */
    private String modifiePar;
}
