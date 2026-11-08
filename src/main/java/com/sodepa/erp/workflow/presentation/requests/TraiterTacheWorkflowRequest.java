package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.DecisionWorkflow;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO) pour la soumission d'une décision par un approbateur sur une tâche.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraiterTacheWorkflowRequest {

    /**
     * Décision formelle prise par le valideur (APPROUVE, REJETE, ou DEMANDE_REVISION).
     */
    @NotNull(message = "La décision est obligatoire")
    private DecisionWorkflow decision;

    /**
     * Motivation textuelle ou justification apportée par le valideur.
     * Requis obligatoirement en cas de rejet ou de demande de révision.
     */
    private String commentaire;

    /**
     * Identifiant de l'utilisateur effectuant l'action (optionnel si déduit du contexte de sécurité).
     */
    private UUID acteurId;

    /**
     * Identifiant du document GED joint lors de la validation (si requis ou fourni).
     */
    private UUID documentGedId;

    /**
     * Nom ou URI de la pièce jointe téléversée.
     */
    private String pieceJointeUri;

    /**
     * Données JSON représentant les champs modifiés ou enrichis lors de la validation.
     */
    private String champsModifiesJson;
}
