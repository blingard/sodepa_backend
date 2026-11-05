package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.DecisionWorkflow;
import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowTache;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTacheResponse {

    private UUID id;
    private UUID instanceId;
    private String entityName;
    private String entityId;
    private String etapeCode;
    private String etapeIntitule;
    private UUID assigneAId;
    private String assigneANom;
    private StatutWorkflowTache statut;
    private DecisionWorkflow decision;
    private String commentaire;
    private Instant dateCreation;
    private Instant dateEcheance;
    private Instant dateDecision;
}
