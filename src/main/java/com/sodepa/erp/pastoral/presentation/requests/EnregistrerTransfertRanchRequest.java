package com.sodepa.erp.pastoral.presentation.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerTransfertRanchRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerTransfertRanchRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément bovin associé.
     */
    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    /**
     * Identifiant unique technique (UUID) de l'élément ranchdestination associé.
     */
    @NotNull(message = "Le ranch de destination est obligatoire")
    private UUID ranchDestinationId;

    /**
     * Champ dateTransfert : date transfert utilisé pour le pilotage et le suivi dans EnregistrerTransfertRanchRequest.
     */
    private LocalDate dateTransfert;

    /**
     * Champ justificatifRef : justificatif ref utilisé pour le pilotage et le suivi dans EnregistrerTransfertRanchRequest.
     */
    private String justificatifRef;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans EnregistrerTransfertRanchRequest.
     */
    private String observations;
}
