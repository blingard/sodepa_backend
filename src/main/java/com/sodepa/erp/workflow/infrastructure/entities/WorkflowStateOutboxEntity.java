package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Table Outbox de secours stockée dans le périmètre PostgreSQL du moteur de workflow.
 * <p>
 * Lorsque ClickHouse est joignable, les états versionnés sont écrits de manière synchrone
 * directement dans {@code clickhouse_workflow_entity_states}. En cas d'indisponibilité temporaire
 * de ClickHouse, cette table conserve durablement chaque version afin d'assurer la continuité
 * du circuit et un rejeu automatique dès le redémarrage de ClickHouse.
 * </p>
 */
@Entity
@Table(name = "workflows_states_outbox", indexes = {
    @Index(name = "idx_wf_outbox_inst_ver", columnList = "instance_id, version"),
    @Index(name = "idx_wf_outbox_synced", columnList = "synced, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStateOutboxEntity {

    @Id
    private UUID id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    @Column(name = "entity_pk", length = 120)
    private String entityPk;

    @Column(name = "operation_type", nullable = false, length = 30)
    private String operationType;

    @Column(name = "definition_code", length = 120)
    private String definitionCode;

    @Column(name = "etape_ordre")
    private Integer etapeOrdre;

    @Column(name = "etape_code", length = 100)
    private String etapeCode;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "acteur_user_id", length = 64)
    private String acteurUserId;

    @Column(name = "acteur_username", length = 150)
    private String acteurUsername;

    @Column(name = "acteur_poste", length = 150)
    private String acteurPoste;

    @Column(name = "patch_json", columnDefinition = "TEXT")
    private String patchJson;

    @Column(name = "full_snapshot_json", columnDefinition = "TEXT", nullable = false)
    private String fullSnapshotJson;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "synced", nullable = false)
    @Builder.Default
    private boolean synced = false;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "synced_at")
    private Instant syncedAt;
}
