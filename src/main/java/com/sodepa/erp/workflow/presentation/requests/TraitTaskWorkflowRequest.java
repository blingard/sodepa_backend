package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDecision;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO) pour la soumission d'une décision par un approbateur sur une tâche.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraitTaskWorkflowRequest {
    @NotNull
    private UUID workflowTaskId;

    /**
     * Décision formelle prise par le valideur (APPROUVE, REJETE, ou DEMANDE_REVISION).
     */
    @NotNull(message = "La décision est obligatoire")
    private WorkflowDecision decision;

    /**
     * Motivation textuelle ou justification apportée par le valideur.
     * Requis obligatoirement en cas de rejet ou de demande de révision.
     */
    private String commentaire;

    private boolean accept;
    private MultipartFile file;
    /**
     * Données JSON représentant les champs modifiés ou enrichis lors de la validation.
     */
    private Map<String, Object> champsModifiesJson;
}
