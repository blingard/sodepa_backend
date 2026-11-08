package com.sodepa.erp.rh.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ApprobationContratDgRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprobationContratDgRequest {
    /**
     * Champ referenceNoteServiceDg : reference note service dg utilisé pour le pilotage et le suivi dans ApprobationContratDgRequest.
     */
    @NotBlank(message = "La référence de la Note de Service ou Circulaire du DG est obligatoire")
    private String referenceNoteServiceDg;

    /**
     * Champ dateSignature : date signature utilisé pour le pilotage et le suivi dans ApprobationContratDgRequest.
     */
    private LocalDate dateSignature;

    /**
     * Commentaires et observations opérationnelles dans le cadre des opérations SODEPA.
     */
    private String commentaire;
}
