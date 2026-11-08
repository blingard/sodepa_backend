package com.sodepa.erp.organigramme.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fiche enrichie d'unité opérationnelle, rattachements fonctionnels multiples et historisation des évolutions
 * (renommage à date d'effet, fusion, scission, fermeture temporaire) — REF-02 / F-REF-008 à F-REF-011.
 */
@Entity
@Table(name = "org_evolutions_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolutionStructureOrganigrammeEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique du unité organisationnelle (Direction, Ranch, Abattoir, Service).
     */
    @Column(name = "code_unite", nullable = false, length = 50)
    private String codeUnite;

    /**
     * Référence obligatoire au texte organique (CA-REF-008-03 / RG-REF-017) :
     * ex. "Résolution n° 11/CA/SODEPA/2018".
     */
    @Column(name = "reference_texte_organique", nullable = false, length = 150)
    @Builder.Default
    private String referenceTexteOrganique = "Résolution n° 11/CA/SODEPA/2018";

    /**
     * Responsable désigné obligatoire de l'unité opérationnelle (CA-REF-010-01 & CA-REF-010-02).
     */
    @Column(name = "responsable_username", nullable = false, length = 100)
    private String responsableUsername;

    /**
     * Rattachements fonctionnels multiples avec justification (CA-REF-008-02).
     * Ex: "DAF (Contrôle budgétaire et régie), DPA (Supervision zootechnique)".
     */
    @Column(name = "rattachements_fonctionnels", length = 500)
    private String rattachementsFonctionnels;

    /**
     * Centres de responsabilité / coûts, magasins, caisses et comptes bancaires rattachés (CA-REF-010-01).
     */
    @Column(name = "centres_couts_rattaches", length = 300)
    private String centresCoutsRattaches;

    /**
     * Magasins rattaches dans le contexte du traitement EvolutionStructureOrganigrammeEntity.
     */
    @Column(name = "magasins_rattaches", length = 300)
    private String magasinsRattaches;

    /**
     * Caisses rattachees dans le contexte du traitement EvolutionStructureOrganigrammeEntity.
     */
    @Column(name = "caisses_rattachees", length = 300)
    private String caissesRattachees;

    /**
     * Compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "comptes_bancaires_rattaches", length = 300)
    private String comptesBancairesRattaches;

    /**
     * Type d'évolution : CONSTITUTION_INITIALE, RENOMMAGE_DATE_EFFET, FUSION_UNITES, SCISSION_UNITE, FERMETURE_TEMPORAIRE.
     */
    @Column(name = "type_evolution", nullable = false, length = 40)
    @Builder.Default
    private String typeEvolution = "CONSTITUTION_INITIALE";

    /**
     * Ancien intitule dans le contexte du traitement EvolutionStructureOrganigrammeEntity.
     */
    @Column(name = "ancien_intitule", length = 180)
    private String ancienIntitule;

    /**
     * Nouvel intitule dans le contexte du traitement EvolutionStructureOrganigrammeEntity.
     */
    @Column(name = "nouvel_intitule", nullable = false, length = 180)
    private String nouvelIntitule;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_effet", nullable = false)
    @Builder.Default
    private LocalDate dateEffet = LocalDate.now();

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_debut_fermeture_temporaire")
    private LocalDate dateDebutFermetureTemporaire;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_reouverture_prevue")
    private LocalDate dateReouverturePrevue;

    /**
     * Rapport d'impact généré lors d'une fusion/scission (utilisateurs, budgets, caisses, circuits) — CA-REF-011-02.
     */
    @Column(name = "rapport_impact_json", columnDefinition = "TEXT")
    private String rapportImpactJson;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
