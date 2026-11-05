package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.DecisionWorkflow;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraiterTacheWorkflowRequest {

    @NotNull(message = "La décision est obligatoire")
    private DecisionWorkflow decision;

    private String commentaire;

    private UUID acteurId;
}
