package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO représentant une opération / activité métier du référentiel universel de workflow.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowMetierResponse {

    /** Identifiant UUID de l'opération métier. */
    private UUID id;

    /** Code fonctionnel unique (ex: "RECRUITMENT", "SERVICE_MEMO"). */
    private String code;

    /** Intitulé lisible. */
    private String title;

    /** Description détaillée. */
    private String description;

    /** Domaine fonctionnel (RH, BUDGET, COMPTABILITE, etc.). */
    private WorkflowDomaine domain;

    /** Indicateur de transversalité (true = ouvert à toutes les unités). */
    private boolean isTransversal;

    /** Identifiant de l'unité autorisée en cas d'exclusivité métier. */
    private UUID authorizedUnitId;

    /** Nom de l'unité autorisée en cas d'exclusivité. */
    private String authorizedUnitName;

    /** État actif ou inactif. */
    private boolean isActive;

    /** Date de création. */
    private Instant createdAt;
}
