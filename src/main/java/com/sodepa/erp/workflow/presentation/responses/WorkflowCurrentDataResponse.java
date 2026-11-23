package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Représentation consolidée des données courantes d'une instance de workflow
 * avant sa matérialisation finale dans les tables métiers PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCurrentDataResponse {

    private UUID instanceId;
    private String entityName;
    private String entityId;
    private String operationType;
    private StatutWorkflowInstance statutWorkflow;
    private String etapeCouranteCode;
    private int version;
    private String lastEventType;
    private String storageSource;
    private boolean syncedToClickHouse;
    private boolean materializedInPostgres;
    private Instant lastUpdatedAt;
    private Map<String, Object> data;
    private List<Map<String, Object>> attachments;
}
