package com.sodepa.erp.workflow.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO) pour la soumission d'une demande de démarrage
 * d'un circuit de validation dans le moteur universel SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemarrerWorkflowRequest {

    /**
     * Identifiant UUID optionnel de la définition de workflow ciblée.
     * Doit être fourni si {@link #workflowDefinitionCode} n'est pas renseigné.
     */
    private UUID workflowDefinitionId;

    /**
     * Code fonctionnel optionnel de la définition de workflow ciblée (ex: "WF-BUDGET-ENGAGEMENT").
     * Doit être fourni si {@link #workflowDefinitionId} n'est pas renseigné.
     */
    private String workflowDefinitionCode;

    /**
     * Typologie fonctionnelle de l'entité métier rattachée (ex: "BON_COMMANDE", "ENGAGEMENT_BUDGETAIRE", "CONGE").
     */
    @NotBlank(message = "Le type d'entité est obligatoire (ex: BON_COMMANDE, ENGAGEMENT_BUDGETAIRE)")
    private String entityName;

    /**
     * Clé primaire / identifiant technique de l'entité métier ciblée.
     */
    @NotBlank(message = "L'identifiant de l'entité est obligatoire")
    private String entityId;

    /**
     * Identifiant UUID du collaborateur qui initie la démarche dans l'ERP.
     */
    @NotNull(message = "L'initiateur est obligatoire")
    private UUID initiateurId;

    /**
     * Contexte métier optionnel sous forme de dictionnaire clé-valeur
     * (montant en FCFA, centre de coût, type d'unité opérationnelle, etc.)
     * utilisé pour l'évaluation dynamique des règles de transition SpEL.
     */
    private Map<String, Object> contexte;
}
