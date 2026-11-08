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

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @NotBlank
    private String intitule;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    private String description;

    /**
     * Champ domaine : domaine utilisé pour le pilotage et le suivi dans ModifierWorkflowDefinitionRequest.
     */
    @NotNull
    private WorkflowDomaine domaine;

    /**
     * Identifiant unique technique (UUID) de l'élément metier associé.
     */
    private UUID metierId;
    /**
     * Identifiant unique technique (UUID) de l'élément uniteorganisationnelle associé.
     */
    private UUID uniteOrganisationnelleId;

    /**
     * Champ rolesInitiateurs : roles initiateurs utilisé pour le pilotage et le suivi dans ModifierWorkflowDefinitionRequest.
     */
    private String rolesInitiateurs;
    /**
     * Champ postesInitiateurs : postes initiateurs utilisé pour le pilotage et le suivi dans ModifierWorkflowDefinitionRequest.
     */
    private String postesInitiateurs;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    private Boolean actif;

    private List<CreerWorkflowDefinitionRequest.CreerEtapeDefinitionRequest> etapes;
}
