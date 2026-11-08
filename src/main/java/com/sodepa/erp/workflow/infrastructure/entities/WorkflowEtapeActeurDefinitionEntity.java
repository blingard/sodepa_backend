package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entité JPA représentant la configuration d'un acteur / validateur attendu au sein d'une étape de workflow.
 * <p>
 * Permet de définir pour chaque étape d'un circuit :
 * <ul>
 *   <li>L'action précise attendue (ex: "Visa Budgétaire", "Signature Note de Service", "Avis Technique")</li>
 *   <li>La désignation nominative de l'employé ({@link #employe}) ou de l'utilisateur ({@link #utilisateur})</li>
 *   <li>La restriction à une unité organisationnelle ({@link #uniteOrganisationnelle})</li>
 *   <li>La stratégie d'assignation dynamique (rôle, poste, manager)</li>
 *   <li>Le caractère obligatoire ({@link #obligatoire} = true) ou consultatif/optionnel de son visa</li>
 *   <li>Le délai SLA spécifique éventuel alloué à cet acteur</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "workflows_etapes_acteurs_definitions", indexes = {
    @Index(name = "idx_wf_acteur_etape", columnList = "etape_definition_id"),
    @Index(name = "idx_wf_acteur_user", columnList = "utilisateur_id"),
    @Index(name = "idx_wf_acteur_employe", columnList = "employe_id"),
    @Index(name = "idx_wf_acteur_unite", columnList = "unite_organisationnelle_id")
})
/**
 * Entité de persistance représentant `WorkflowEtapeActeurDefinitionEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEtapeActeurDefinitionEntity {

    /**
     * Identifiant unique universel (UUID) de la définition de l'acteur.
     */
    @Id
    private UUID id;

    /**
     * Étape de workflow parente à laquelle cet acteur est rattaché.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_definition_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeDefinition;

    /**
     * Libellé ou description explicite de l'action attendue de cet acteur
     * (ex: "Visa Soutenabilité Financière", "Avis Sanitaire Vétérinaire", "Signature DG").
     */
    @Column(name = "intitule_action", nullable = false, length = 150)
    private String intituleAction;

    /**
     * Employé spécifique nommément désigné (Clé métier).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id")
    private EmployeEntity employe;

    /**
     * Utilisateur spécifique nommément désigné (Liaison compte technique).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private UtilisateurEntity utilisateur;

    /**
     * Unité organisationnelle d'appartenance exigée pour le validateur (ex: Cellule Informatique).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Stratégie de résolution dynamique si aucun employé fixe n'est directement renseigné
     * (ex: POSTE_SPECIFIQUE, ROLE_KEYCLOAK, MANAGER_N1, RESPONSABLE_STRUCTURE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategie_assignation", nullable = false, length = 40)
    private StrategieAssignation strategieAssignation;

    /**
     * Valeur de consigne associée à la stratégie d'assignation
     * (ex: "ROLE_DAF", "SOUS_DIRECTEUR_RH", "CHEF_RANCH_FARO").
     */
    @Column(name = "valeur_assignation", length = 100)
    private String valeurAssignation;

    /**
     * Poids du validateur : indique si l'approbation de cet acteur est indispensable pour valider l'étape.
     * <ul>
     *   <li>{@code true} (REQUIRED) : L'étape ne peut JAMAIS être franchie tant que cet acteur n'a pas validé.
     *       Tout rejet de sa part entraîne le rejet immédiat du dossier.</li>
     *   <li>{@code false} (OPTIONAL) : Avis consultatif ou concourant à la politique de majorité.</li>
     * </ul>
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean obligatoire = true;

    /**
     * Délai limite SLA spécifique en heures pour cet acteur (si différent du délai global de l'étape).
     */
    @Column(name = "delai_sla_heures")
    private Integer delaiSlaHeures;

    /**
     * Action automatique en cas de dépassement SLA pour cet acteur.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_depassement_sla", nullable = false, length = 40)
    @Builder.Default
    private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

    /**
     * Indicateur d'activation de ce validateur. Si faux, aucune tâche ne lui est générée.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean actif = true;
}
