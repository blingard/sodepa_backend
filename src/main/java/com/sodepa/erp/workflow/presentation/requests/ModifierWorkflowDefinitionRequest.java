package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Requête pour modifier une définition de circuit de workflow existante.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierWorkflowDefinitionRequest {

    @NotBlank
    private String intitule;

    private String description;

    @NotNull
    private WorkflowDomaine domaine;

    private UUID metierId;
    private UUID uniteOrganisationnelleId;

    private String rolesInitiateurs;
    private String postesInitiateurs;

    private Boolean actif;

    private List<CreerWorkflowDefinitionRequest.CreerEtapeDefinitionRequest> etapes;
}
