package com.sodepa.erp.audit.application.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Instantané versionné de l'état consolidé d'un dossier métier en cours de circuit workflow,
 * persisté de manière synchrone dans ClickHouse (table {@code clickhouse_workflow_entity_states})
 * avec bascule automatique sur l'Outbox PostgreSQL en cas d'indisponibilité ClickHouse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEntityStateSnapshot {

    private UUID id;
    private UUID instanceId;
    private int version;
    private String eventType;
    private String entityName;
    private String entityPk;
    private String operationType;
    private String definitionCode;
    private Integer etapeOrdre;
    private String etapeCode;
    private UUID taskId;
    private String acteurUserId;
    private String acteurUsername;
    private String acteurPoste;
    private String patchJson;
    private String fullSnapshotJson;
    private String attachmentsJson;
    private String commentaire;
    private Instant createdAt;
    private String storageSource;
}
