package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA représentant une tâche d'action individuelle assignée à un approbateur précis.
 * <p>
 * Une tâche associe un employé valideur ({@link #assigneAEmploye}) au sein de son unité organisationnelle
 * et un compte utilisateur ({@link #assigneA}) pour les sessions et notifications.
 * </p>
 */
@Entity
@Table(name = "workflows_taches", indexes = {
    @Index(name = "idx_wf_tache_inst", columnList = "workflow_instance_id"),
    @Index(name = "idx_wf_tache_assigne", columnList = "assigne_utilisateur_id"),
    @Index(name = "idx_wf_tache_employe", columnList = "assigne_employe_id"),
    @Index(name = "idx_wf_tache_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTacheEntity {

    /**
     * Identifiant unique universel (UUID) de la tâche de workflow.
     */
    @Id
    private UUID id;

    /**
     * Instance de workflow globale dont relève cette tâche spécifique.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstanceEntity workflowInstance;

    /**
     * Étape de définition à laquelle se rattache cette obligation d'approbation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_definition_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeDefinition;

    /**
     * Définition spécifique de l'acteur et de l'action attendue pour cette tâche.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_acteur_id")
    private WorkflowEtapeActeurDefinitionEntity etapeActeur;

    /**
     * Intitulé de l'action attendue (ex: "Visa Soutenabilité Financière").
     */
    @Column(name = "intitule_action", length = 150)
    private String intituleAction;

    /**
     * Poids du validateur (copie snapshot lors de l'instanciation de la tâche) :
     * Si true (REQUIRED), cette tâche doit impérativement être APPROUVEE pour que l'étape avance.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean obligatoire = true;

    /**
     * Employé assigné à la tâche (Clé métier centrale, matricule, unité, poste).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_employe_id")
    private EmployeEntity assigneAEmploye;

    /**
     * Utilisateur désigné pour traiter cette tâche (liaison technique pour l'authentification et session).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_utilisateur_id", nullable = false)
    private UtilisateurEntity assigneA;

    /**
     * Statut actuel de la tâche (EN_ATTENTE, APPROUVEE, REJETEE, DELEGUEE, EXPIREE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutWorkflowTache statut = StatutWorkflowTache.EN_ATTENTE;

    /**
     * Décision finale enregistrée par le valideur lors de son traitement.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DecisionWorkflow decision;

    /**
     * Commentaire explicatif, réserves ou observations formulées par le valideur.
     */
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    /**
     * Horodatage de génération et d'attribution de la tâche au valideur.
     */
    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    /**
     * Date et heure limites de traitement selon le délai SLA configuré pour l'étape.
     */
    @Column(name = "date_echeance")
    private Instant dateEcheance;

    /**
     * Date et heure effectives auxquelles le valideur a soumis sa décision.
     */
    @Column(name = "date_decision")
    private Instant dateDecision;
}
