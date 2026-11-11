package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA matérialisant une exécution concrète d'un circuit de validation pour un objet métier SODEPA.
 * <p>
 * L'instance encapsule :
 * <ul>
 *   <li>La référence au modèle de circuit appliqué ({@code workflowDefinition})</li>
 *   <li>L'objet métier ciblé de façon polymorphe et agnostique ({@code entityName} + {@code entityId})</li>
 *   <li>L'employé initiateur de la demande ({@link #initiateurEmploye})</li>
 *   <li>L'unité organisationnelle émettrice pour le cloisonnement de sécurité ({@link #uniteOrganisationnelle})</li>
 *   <li>L'état d'avancement global (EN_COURS, APPROUVE, REJETE, SUSPENDU, ANNULE)</li>
 *   <li>L'étape courante active</li>
 *   <li>Les données contextuelles sérialisées en JSON</li>
 *   <li>La liste des tâches générées pour les valideurs</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "workflows_instances", indexes = {
    @Index(name = "idx_wf_inst_entity", columnList = "entity_name, entity_id"),
    @Index(name = "idx_wf_inst_statut", columnList = "statut"),
    @Index(name = "idx_wf_inst_initiateur", columnList = "initiateur_id"),
    @Index(name = "idx_wf_inst_employe", columnList = "initiateur_employe_id"),
    @Index(name = "idx_wf_inst_poste", columnList = "poste_id"),
    @Index(name = "idx_wf_inst_unite", columnList = "unite_organisationnelle_id")
})
/**
 * Entité de persistance représentant `WorkflowInstanceEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "WFI";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique universel (UUID) de l'instance de workflow.
     */
    @Id
    private UUID id;

    /**
     * Numéro de version pour le verrouillage optimiste anti-concurrence (D7).
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Modèle de définition de circuit régissant cette instance d'exécution.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinitionEntity workflowDefinition;

    /**
     * Nom fonctionnel de la typologie d'entité métier ciblée (ex: "com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity").
     */
    @Column(name = "entity_name", nullable = false, length = 60)
    private String entityName;

    /**
     * Identifiant technique unique de l'entité métier ciblée (clé primaire UUID sous forme de chaîne).
     */
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    /**
     * Employé initiateur ayant soumis le dossier (Clé métier centrale).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiateur_employe_id")
    private EmployeEntity initiateurEmploye;

    /**
     * Compte utilisateur technique de l'initiateur (pour la session et traçabilité IAM).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiateur_id", nullable = false)
    private UtilisateurEntity initiateur;

    /**
     * Unité organisationnelle d'appartenance de l'initiateur au moment de la soumission.
     * Sert de base au cloisonnement territorial et cellulaire des dossiers.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Unité organisationnelle d'appartenance de l'initiateur au moment de la soumission.
     * Sert de base au cloisonnement territorial et cellulaire des dossiers.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private PosteEntity poste;

    /**
     * Statut général d'avancement du cycle de vie de l'instance.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutWorkflowInstance statut = StatutWorkflowInstance.EN_COURS;

    /**
     * Étape actuellement active au sein du circuit, en attente d'approbation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_courante_id")
    private WorkflowEtapeDefinitionEntity etapeCourante;

    /**
     * Données contextuelles et métadonnées sérialisées sous format JSON
     * (utilisées pour évaluer les règles d'aiguillage conditionnel SpEL).
     */
    @Column(name = "donnees_contexte", columnDefinition = "TEXT")
    private String donneesContexte;

    /**
     * Horodatage de lancement du circuit.
     */
    @Column(name = "date_initiation", nullable = false)
    private Instant dateInitiation;

    /**
     * Horodatage de clôture définitive (approbation finale ou rejet).
     */
    @Column(name = "date_cloture")
    private Instant dateCloture;

    /**
     * Liste des tâches individuelles générées tout au long du cycle de vie de cette instance.
     */
    @Builder.Default
    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTaskEntity> taches = new ArrayList<>();
}
