package com.sodepa.erp.workflow.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * DTO de soumission pour le démarrage d'un workflow par opération métier et employé.
 * Évite d'écrire en dur les codes de workflow dans le code applicatif.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemarrerWorkflowMetierRequest {

    /** Code métier universel de l'opération (ex: "RECRUITMENT", "SERVICE_MEMO", "BUDGET_COMMITMENT"). */
    @NotBlank(message = "Le code métier est obligatoire")
    private String codeMetier;

    /** Type de l'entité métier ciblée (ex: "EMPLOYE", "NOTE_SERVICE", "ENGAGEMENT_BUDGETAIRE"). */
    @NotBlank(message = "Le type d'entité est obligatoire")
    private String entityName;

    /** Identifiant de l'entité métier ciblée. */
    @NotBlank(message = "L'identifiant de l'entité est obligatoire")
    private String entityId;

    /** Identifiant de l'employé initiateur (Maker). */
    @NotNull(message = "L'employé initiateur est obligatoire")
    private UUID employeId;

    /** Contexte et métadonnées pour les conditions d'aiguillage éventuelles. */
    private Map<String, Object> contexte;
}
