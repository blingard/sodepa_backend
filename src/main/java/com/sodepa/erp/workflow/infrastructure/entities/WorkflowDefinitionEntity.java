package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA représentant un modèle (définition) de circuit de validation dans l'ERP SODEPA.
 * <p>
 * Un modèle de workflow définit la séquence ordonnée d'étapes à franchir pour une opération métier donnée
 * et est rattaché à une unité organisationnelle propriétaire ({@link #uniteOrganisationnelle}).
 * </p>
 */
@Entity
@Table(name = "workflows_definitions", indexes = {
    @Index(name = "idx_wf_def_code", columnList = "code", unique = true),
    @Index(name = "idx_wf_def_domaine", columnList = "domaine"),
    @Index(name = "idx_wf_def_metier", columnList = "metier_id"),
    @Index(name = "idx_wf_def_unite", columnList = "unite_organisationnelle_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionEntity {

    /**
     * Identifiant unique universel (UUID) de la définition de workflow.
     */
    @Id
    private UUID id;

    /**
     * Opération métier référencée (ex: RECRUITMENT, SERVICE_MEMO, BUDGET_COMMITMENT).
     * Permet la résolution dynamique du circuit sans code en dur.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metier_id")
    private WorkflowMetierEntity metier;

    /**
     * Unité organisationnelle propriétaire de ce circuit de validation.
     * Chaque unité (Cellule Informatique, Cellule Comptabilité, Ranch Faro...) possède son propre circuit.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Code fonctionnel unique et standardisé du circuit (ex: "WF-INFO-MEMO", "WF-COMPTA-MEMO").
     */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    /**
     * Intitulé lisible et explicite du circuit de validation pour les utilisateurs de l'ERP.
     */
    @Column(nullable = false, length = 150)
    private String intitule;

    /**
     * Description détaillée des objectifs métier, des seuils applicables et des services concernés.
     */
    @Column(columnDefinition = "TEXT", length = 1000)
    private String description;

    /**
     * Domaine d'application métier (Budget, RH, Achats, Ranches, Abattoirs, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkflowDomaine domaine;

    /**
     * Numéro de version du circuit de validation (incrémenté lors des modifications structurelles).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Indicateur d'activation du circuit. Un circuit inactif ne peut plus recevoir de nouvelles instances.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    /**
     * Rôles Keycloak habilités à initier ce workflow en tant que Maker (ex: "ROLE_DRH,ROLE_GESTIONNAIRE_RH").
     */
    @Column(name = "roles_initiateurs", length = 500)
    private String rolesInitiateurs;

    /**
     * Postes organisationnels habilités à initier ce workflow en tant que Maker (ex: "SOUS_DIRECTEUR_RH,GESTIONNAIRE_RH").
     */
    @Column(name = "postes_initiateurs", length = 500)
    private String postesInitiateurs;

    /**
     * Liste ordonnée des étapes constitutives du circuit de validation.
     */
    @Builder.Default
    @OneToMany(mappedBy = "workflowDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<WorkflowEtapeDefinitionEntity> etapes = new ArrayList<>();

    /**
     * Horodatage de création de cette définition de circuit dans l'ERP.
     */
    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    /**
     * Ajoute une étape à la définition en maintenant la cohérence bidirectionnelle de la relation JPA.
     *
     * @param etape L'entité d'étape de workflow à rattacher à ce circuit.
     */
    public void addEtape(WorkflowEtapeDefinitionEntity etape) {
        if (this.etapes == null) {
            this.etapes = new ArrayList<>();
        }
        this.etapes.add(etape);
        etape.setWorkflowDefinition(this);
    }
}
