package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA de la piste d'audit infalsifiable du moteur de workflow universel SODEPA.
 * <p>
 * Enregistre de manière immuable chaque action survenue au cours du traitement d'un dossier :
 * initiation, approbations successives, réserves, demandes d'éclaircissements, délégations,
 * escalades SLA et clôture finale. Permet de répondre aux exigences de traçabilité des commissaires aux comptes.
 * </p>
 */
@Entity
@Table(name = "workflows_historique_audit", indexes = {
    @Index(name = "idx_wf_audit_inst", columnList = "workflow_instance_id"),
    @Index(name = "idx_wf_audit_horodate", columnList = "horodatage")
})
/**
 * Entité de persistance représentant `WorkflowHistoriqueAuditEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowHistoriqueAuditEntity {

    /**
     * Identifiant unique universel (UUID) de l'enregistrement d'audit.
     */
    @Id
    private UUID id;

    /**
     * Référence vers l'instance d'exécution de workflow concernée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstanceEntity workflowInstance;

    /**
     * Code de l'étape sur laquelle l'action a été effectuée (ex: "VISA_N1", "CONTROLE_DAF").
     */
    @Column(name = "etape_code", nullable = false, length = 60)
    private String etapeCode;

    /**
     * Identifiant de l'utilisateur (ou du service d'arrière-plan) ayant posé l'action.
     */
    @Column(name = "acteur_id", nullable = false)
    private UUID acteurId;

    /**
     * Nature formelle de l'opération d'audit (INITIATION, APPROBATION, REJET, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActionWorkflowAudit action;

    /**
     * Décision prise lors de la tâche le cas échéant (APPROUVE, REJETE, DEMANDE_REVISION).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WorkflowDecision decision;

    /**
     * Commentaire ou motivation textuelle fournie par l'opérateur (obligatoire en cas de rejet).
     */
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    /**
     * Identifiant technique de corrélation pour le traçage distribué des requêtes (logs / métriques).
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /**
     * Horodatage UTC précis de l'action enregistrée.
     */
    @Column(nullable = false)
    private Instant horodatage;
}
