package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowInstance;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceResponse {

    private UUID id;
    private UUID definitionId;
    private String definitionCode;
    private String definitionIntitule;
    private String entityName;
    private String entityId;
    private UUID initiateurId;
    private String initiateurNom;
    private StatutWorkflowInstance statut;
    private String etapeCouranteCode;
    private String etapeCouranteIntitule;
    private Instant dateInitiation;
    private Instant dateCloture;
    private List<WorkflowTacheResponse> taches;
}
