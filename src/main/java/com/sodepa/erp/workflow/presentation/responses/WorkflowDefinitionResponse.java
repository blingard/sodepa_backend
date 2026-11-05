package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionResponse {

    private UUID id;
    private String code;
    private String intitule;
    private String description;
    private WorkflowDomaine domaine;
    private Integer version;
    private boolean actif;
    private List<WorkflowEtapeResponse> etapes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkflowEtapeResponse {
        private UUID id;
        private Integer ordre;
        private String codeEtape;
        private String intitule;
        private String typeEtape;
        private String politiqueValidation;
        private String strategieAssignation;
        private String valeurAssignation;
        private Integer delaiSlaHeures;
    }
}
