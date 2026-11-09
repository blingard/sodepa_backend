package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
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
    @Index(name = "idx_wf_tache_poste", columnList = "assigne_poste_id"),
    @Index(name = "idx_wf_tache_statut", columnList = "statut")
})
/**
 * Entité de persistance représentant `WorkflowTacheEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTaskEntity {

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

    private String notificationTemplate;

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


    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isDelegated = false;

    /**
     * Employé assigné à la tâche (Clé métier centrale, matricule, unité, poste).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_employe_id")
    private EmployeEntity assigneEmploye;

    /**
     * Utilisateur désigné pour traiter cette tâche (liaison technique pour l'authentification et session).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_utilisateur_id", nullable = false)
    private UtilisateurEntity assigneUser;

    /**
     * Utilisateur désigné pour traiter cette tâche (liaison technique pour l'authentification et session).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_poste_id")
    private PosteEntity assignePoste;

    /**
     * Statut actuel de la tâche (EN_ATTENTE, APPROUVEE, REJETEE, DELEGUEE, EXPIREE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutWorkflowTache statut = StatutWorkflowTache.PENDING;

    /**
     * Décision finale enregistrée par le valideur lors de son traitement.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WorkflowDecision decision;

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

    @Column(name = "date_decision")
    private Instant dateDecision;

    @Column(name = "entity_package_reference", length = 150)
    private String entityPackageReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_json_data", columnDefinition = "jsonb")
    private Map<String, Object> currentData;

    @Column(name = "is_last_task_of_step", nullable = false, columnDefinition = "boolean default false")
    private boolean isLastTaskOfStep;

    @Column(name = "is_first_task_of_step", nullable = false, columnDefinition = "boolean default false")
    private boolean isFirstTaskOfStep;

    /**
     * Mode d'action exigé ou permis pour l'employé validateur lors du traitement de cette étape
     * (RIEN_A_AJOUTER, AJOUT_FICHIER_OBLIGATOIRE, MODIFICATION_CHAMPS_OBLIGATOIRE, FICHIER_ET_CHAMPS_OBLIGATOIRES, OPTIONNEL_FICHIER_OU_CHAMPS).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_action_validation", length = 40)
    @Builder.Default
    private ValidationStepActionMode modeActionValidation = ValidationStepActionMode.ADD_FILE;

    // --- Alias de rétrocompatibilité d'accès ---
    public EmployeEntity getAssigneAEmploye() {
        return this.assigneEmploye;
    }

    public void setAssigneAEmploye(EmployeEntity emp) {
        this.assigneEmploye = emp;
    }

    public UtilisateurEntity getAssigneA() {
        return this.assigneUser;
    }

    public void setAssigneA(UtilisateurEntity user) {
        this.assigneUser = user;
    }

    public static class WorkflowTaskEntityBuilder {
        public WorkflowTaskEntityBuilder assigneAEmploye(EmployeEntity emp) {
            this.assigneEmploye = emp;
            return this;
        }

        public WorkflowTaskEntityBuilder assigneA(UtilisateurEntity user) {
            this.assigneUser = user;
            return this;
        }
    }

    public UUID getDocumentGedId() {
        return null;
    }

    public String getPieceJointeUri() {
        if (currentData != null && currentData.containsKey("fileUrl")) {
            return String.valueOf(currentData.get("fileUrl"));
        }
        return null;
    }

    public String getChampsModifiesJson() {
        if (currentData != null && id != null && currentData.containsKey(id.toString())) {
            return String.valueOf(currentData.get(id.toString()));
        }
        return null;
    }
}
