package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA modélisant une étape spécifique au sein d'une définition de circuit de workflow.
 * <p>
 * Chaque étape spécifie :
 * <ul>
 *   <li>Son rang séquentiel dans le flux d'exécution ({@code ordre})</li>
 *   <li>La nature de l'action requise (humaine, signature électronique, contrôle système)</li>
 *   <li>La politique d'approbation (approbation unique, consensus, majorité)</li>
 *   <li>La liste des acteurs attendus ({@link #acteurs}) avec leurs rôles/postes et leur poids obligatoire</li>
 *   <li>Les délais limites SLA et les mesures d'escalade en cas de retard</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "workflows_etapes_definitions", indexes = {
    @Index(name = "idx_wf_etape_wf", columnList = "workflow_definition_id"),
    @Index(name = "idx_wf_etape_code", columnList = "code_etape")
})
/**
 * Entité de persistance représentant `WorkflowEtapeDefinitionEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEtapeDefinitionEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "WED";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique universel (UUID) de l'étape.
     */
    @Id
    private UUID id;

    /**
     * Définition parente du circuit de validation auquel cette étape appartient.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinitionEntity workflowDefinition;

    /**
     * Numéro d'ordre séquentiel d'exécution de l'étape (1, 2, 3...).
     */
    @Column(nullable = false)
    private Integer ordre;

    /**
     * Code fonctionnel unique au sein du circuit (ex: "VISA_CHEF_SERVICE", "VISA_DAF", "SIGNATURE_DG").
     */
    @Column(name = "code_etape", nullable = false, length = 60)
    private String codeEtape;



    @Column(name = "entity_package_reference", length = 150)
    private String entityPackageReference;

    /**
     * Libellé explicite de l'étape présenté à l'utilisateur dans l'interface de validation.
     */
    @Column(nullable = false, length = 150)
    private String intitule;

    /**
     * Typologie de l'étape : validation humaine manuelle, signature qualifiée, contrôle automatique ou notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_etape", nullable = false, length = 40)
    private TypeEtapeWorkflow typeEtape;


    @Column(name = "is_last_task_of_step", nullable = false, columnDefinition = "boolean default false")
    private boolean isLastTaskOfStep;

    @Column(name = "is_first_task_of_step", nullable = false, columnDefinition = "boolean default false")
    private boolean isFirstTaskOfStep;

    /**
     * Mode d'action exigé ou permis pour l'employé validateur lors du traitement de cette étape
     * (DO_NOTHING, ADD_FILE, SET_ATTRIBUTE, FILE_AND_ATTRIBUTE, OPTIONAL_FILE_OR_ATTRIBUTE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_action_validation", length = 40)
    @Builder.Default
    private ValidationStepActionMode modeActionValidation = ValidationStepActionMode.OPTIONAL_FILE_OR_ATTRIBUTE;


    /**
     * Autorise ou interdit le téléversement d'un document justificatif (GED) lors de la validation.
     */
    @Column(name = "autoriser_ajout_fichier")
    @Builder.Default
    private Boolean autoriserAjoutFichier = true;

    /**
     * Rend obligatoire le téléversement d'un fichier pour pouvoir soumettre la validation.
     */
    @Column(name = "fichier_validation_obligatoire")
    @Builder.Default
    private Boolean fichierValidationObligatoire = false;

    /**
     * Règle de consensus lorsque plusieurs valideurs sont potentiellement éligibles sur cette étape.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "politique_validation", nullable = false, length = 40)
    @Builder.Default
    private PolitiqueValidation politiqueValidation = PolitiqueValidation.ONLY_ONE_CAN_VALIDATE;

    /**
     * Stratégie d'assignation par défaut de l'étape (utilisée en fallback ou pour étapes mono-acteur).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategie_assignation", length = 40)
    private StrategieAssignation strategieAssignation;

    /**
     * Délai maximal de traitement alloué pour cette étape en heures (SLA).
     */
    @Column(name = "delai_sla_heures")
    private Integer delaiSlaHeures;

    /**
     * Mesure corrective automatique à appliquer en cas de dépassement de l'échéance SLA.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_depassement_sla", nullable = false, length = 40)
    @Builder.Default
    private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

    /**
     * Indicateur d'activation de l'étape. Une étape inactive est ignorée ou suspendue.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean actif = true;

    /**
     * Liste des acteurs et validateurs configurés pour intervenir sur cette étape en parallèle.
     */
    @Builder.Default
    @OneToMany(mappedBy = "etapeDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowEtapeActeurDefinitionEntity> acteurs = new ArrayList<>();

    /**
     * Rattache un acteur attendu à cette étape.
     *
     * @param acteur L'acteur de validation à ajouter.
     */
    public void addActeur(WorkflowEtapeActeurDefinitionEntity acteur) {
        if (this.acteurs == null) {
            this.acteurs = new ArrayList<>();
        }
        this.acteurs.add(acteur);
        acteur.setEtapeDefinition(this);
    }

    public boolean isActif() {
        return this.actif;
    }

    public boolean getActif() {
        return this.actif;
    }
}
