package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Requête pour modifier une opération métier dans le registre.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierWorkflowMetierRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private WorkflowDomaine domain;

    private Boolean isTransversal;

    private UUID authorizedUnitId;

    private Boolean isActive;
}
