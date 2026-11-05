package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité de journalisation infalsifiable de toutes les transitions et décisions de workflow.
 */
@Entity
@Table(name = "workflows_historique_audit", indexes = {
    @Index(name = "idx_wf_audit_inst", columnList = "workflow_instance_id"),
    @Index(name = "idx_wf_audit_horodate", columnList = "horodatage")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowHistoriqueAuditEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstanceEntity workflowInstance;

    @Column(name = "etape_code", nullable = false, length = 60)
    private String etapeCode;

    @Column(name = "acteur_id", nullable = false)
    private UUID acteurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActionWorkflowAudit action;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DecisionWorkflow decision;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(nullable = false)
    private Instant horodatage;
}
