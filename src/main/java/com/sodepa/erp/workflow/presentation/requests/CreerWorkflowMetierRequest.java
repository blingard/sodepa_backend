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

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @NotBlank
    private String code;

    /**
     * Champ title : title utilisé pour le pilotage et le suivi dans CreerWorkflowMetierRequest.
     */
    @NotBlank
    private String title;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    private String description;

    /**
     * Champ domain : domain utilisé pour le pilotage et le suivi dans CreerWorkflowMetierRequest.
     */
    @NotNull
    private WorkflowDomaine domain;

    /**
     * Indicateur booléen spécifiant si isTransversal est effectif ou applicable.
     */
    @Builder.Default
    private Boolean isTransversal = false;

    /**
     * Identifiant unique technique (UUID) de l'élément authorizedunit associé.
     */
    private UUID authorizedUnitId;

    /**
     * Indicateur booléen spécifiant si isActive est effectif ou applicable.
     */
    @Builder.Default
    private Boolean isActive = true;
}
