package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Requête pour déclarer une nouvelle opération métier dans le registre transversal SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerWorkflowMetierRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private WorkflowDomaine domain;

    @Builder.Default
    private Boolean isTransversal = false;

    private UUID authorizedUnitId;

    @Builder.Default
    private Boolean isActive = true;
}
