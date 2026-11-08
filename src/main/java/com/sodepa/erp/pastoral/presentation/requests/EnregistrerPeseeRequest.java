package com.sodepa.erp.pastoral.presentation.requests;

import com.sodepa.erp.pastoral.infrastructure.entities.EtatSanteBovin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerPeseeRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerPeseeRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément bovin associé.
     */
    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    /**
     * Champ poidsKg : poids kg utilisé pour le pilotage et le suivi dans EnregistrerPeseeRequest.
     */
    @NotNull(message = "Le poids mesuré est obligatoire")
    private BigDecimal poidsKg;

    /**
     * Champ datePesee : date pesee utilisé pour le pilotage et le suivi dans EnregistrerPeseeRequest.
     */
    private LocalDate datePesee;

    /**
     * Champ etatSante : etat sante utilisé pour le pilotage et le suivi dans EnregistrerPeseeRequest.
     */
    private EtatSanteBovin etatSante;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans EnregistrerPeseeRequest.
     */
    private String observations;
}
