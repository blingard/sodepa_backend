package com.sodepa.erp.workflow.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemarrerWorkflowRequest {

    private UUID workflowDefinitionId;

    private String workflowDefinitionCode;

    @NotBlank(message = "Le type d'entité est obligatoire (ex: BON_COMMANDE, ENGAGEMENT_BUDGETAIRE)")
    private String entityName;

    @NotBlank(message = "L'identifiant de l'entité est obligatoire")
    private String entityId;

    @NotNull(message = "L'initiateur est obligatoire")
    private UUID initiateurId;

    private Map<String, Object> contexte;
}
