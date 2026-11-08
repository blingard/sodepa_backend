package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.utils.Permissions;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `AccorderDerogationRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccorderDerogationRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément utilisateur associé.
     */
    @NotNull(message = "L'utilisateur bénéficiaire est obligatoire")
    private UUID utilisateurId;

    /**
     * Champ permission : permission utilisé pour le pilotage et le suivi dans AccorderDerogationRequest.
     */
    @NotNull(message = "L'habilitation à déroger est obligatoire")
    private Permissions permission;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "La date de début est obligatoire")
    private Instant dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "La date de fin est obligatoire")
    private Instant dateFin;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    private String motif;
    /**
     * Champ referenceDecision : reference decision utilisé pour le pilotage et le suivi dans AccorderDerogationRequest.
     */
    private String referenceDecision;
}
