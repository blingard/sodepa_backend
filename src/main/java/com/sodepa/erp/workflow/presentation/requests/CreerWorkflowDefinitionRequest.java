package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.ActionDepassementSla;
import com.sodepa.erp.workflow.infrastructure.entities.PolitiqueValidation;
import com.sodepa.erp.workflow.infrastructure.entities.StrategieAssignation;
import com.sodepa.erp.workflow.infrastructure.entities.TypeEtapeWorkflow;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Requête pour créer une nouvelle définition de workflow avec ses étapes et validateurs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerWorkflowDefinitionRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String intitule;

    private String description;

    @NotNull
    private WorkflowDomaine domaine;

    private UUID metierId;
    private String metierCode;

    private UUID uniteOrganisationnelleId;

    private String rolesInitiateurs;
    private String postesInitiateurs;

    @Builder.Default
    private Boolean actif = true;

    private List<CreerEtapeDefinitionRequest> etapes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreerEtapeDefinitionRequest {
        @NotNull
        private Integer ordre;

        @NotBlank
        private String codeEtape;

        @NotBlank
        private String intitule;

        @NotNull
        private TypeEtapeWorkflow typeEtape;

        @Builder.Default
        private PolitiqueValidation politiqueValidation = PolitiqueValidation.UN_SEUL_SUFFIT;

        private StrategieAssignation strategieAssignation;
        private String valeurAssignation;
        private Integer delaiSlaHeures;

        @Builder.Default
        private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

        @Builder.Default
        private Boolean actif = true;

        private List<CreerActeurDefinitionRequest> acteurs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreerActeurDefinitionRequest {
        @NotBlank
        private String intituleAction;

        private UUID employeId;
        private UUID utilisateurId;
        private UUID uniteOrganisationnelleId;

        @NotNull
        private StrategieAssignation strategieAssignation;

        private String valeurAssignation;

        @Builder.Default
        private Boolean obligatoire = true;

        private Integer delaiSlaHeures;

        @Builder.Default
        private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

        @Builder.Default
        private Boolean actif = true;
    }
}
