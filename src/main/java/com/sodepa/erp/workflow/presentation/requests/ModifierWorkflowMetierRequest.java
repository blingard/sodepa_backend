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

    /**
     * Champ title : title utilisé pour le pilotage et le suivi dans ModifierWorkflowMetierRequest.
     */
    @NotBlank
    private String title;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    private String description;

    /**
     * Champ domain : domain utilisé pour le pilotage et le suivi dans ModifierWorkflowMetierRequest.
     */
    @NotNull
    private WorkflowDomaine domain;

    /**
     * Indicateur booléen spécifiant si isTransversal est effectif ou applicable.
     */
    private Boolean isTransversal;

    /**
     * Identifiant unique technique (UUID) de l'élément authorizedunit associé.
     */
    private UUID authorizedUnitId;

    /**
     * Indicateur booléen spécifiant si isActive est effectif ou applicable.
     */
    private Boolean isActive;
}
