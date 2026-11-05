package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité représentant une tâche d'approbation ou de visa assignée à un utilisateur.
 */
@Entity
@Table(name = "workflows_taches", indexes = {
    @Index(name = "idx_wf_tache_inst", columnList = "workflow_instance_id"),
    @Index(name = "idx_wf_tache_assigne", columnList = "assigne_utilisateur_id"),
    @Index(name = "idx_wf_tache_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTacheEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstanceEntity workflowInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_definition_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_utilisateur_id", nullable = false)
    private UtilisateurEntity assigneA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutWorkflowTache statut = StatutWorkflowTache.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DecisionWorkflow decision;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_echeance")
    private Instant dateEcheance;

    @Column(name = "date_decision")
    private Instant dateDecision;
}
