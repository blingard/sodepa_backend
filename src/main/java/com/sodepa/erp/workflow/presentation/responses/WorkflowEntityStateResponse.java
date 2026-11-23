package com.sodepa.erp.workflow.presentation.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Représentation d'une version historique d'état dans ClickHouse (ou Outbox PostgreSQL en secours)
 * exposée par {@code GET /api/workflows/instances/{id}/states}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEntityStateResponse {

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
    private Map<String, Object> patch;
    private Map<String, Object> snapshot;
    private List<Map<String, Object>> attachments;
    private String commentaire;
    private String storageSource;
    private boolean syncedToClickHouse;
    private Instant createdAt;
}
