package com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Élément de la nomenclature budgétaire : programme, action, activité ou tâche
 * (F-BUD-002, « élément programmatique » de la spécification, § 4).
 *
 * <p>
 * Une seule table pour les quatre niveaux, reliés par {@code parent_id} : les
 * cumuls par programme, les rapports et l'import du plan de codification
 * (US-BUD-106) parcourent ainsi un seul arbre. Le schéma est porté par le
 * changelog 28.0.
 * </p>
 *
 * <p>
 * Le parent est gardé en identifiant, pas en association JPA : l'arbre entier
 * est lu en une requête et assemblé en mémoire, sans chargement paresseux
 * à chaque niveau.
 * </p>
 *
 * <p>
 * Le code et le parent ne changent plus après la création : des exercices
 * passés les citent, et déplacer une action changerait les cumuls de son
 * programme. Un renommage ou un déplacement passera par l'import d'une
 * nouvelle version du plan (US-BUD-106), qui en garde l'historique.
 * </p>
 */
@Entity
@Table(name = "budget_element_programmatique")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElementProgrammatiqueEntity {

    /** Identifiant, fixé dès la demande de création (voir {@code NomenclatureAdapter}). */
    @Id
    private UUID id;

    /** Code du plan de codification, en majuscules, unique sur tous les niveaux et jamais réutilisé. */
    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private NiveauProgrammatique niveau;

    /** Élément du niveau juste au-dessus ; {@code null} pour un programme. */
    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    /** Utilisateur responsable : obligatoire pour un programme, facultatif en dessous. */
    @Column(name = "responsable_id")
    private UUID responsableId;

    /**
     * Un élément désactivé reste dans l'arbre, pour les exercices qui le
     * citent, mais ne reçoit plus de nouvelle ligne ni de nouvel enfant.
     */
    @Column(nullable = false)
    private boolean actif;

    /** Motif de la dernière désactivation ; effacé à la réactivation. */
    @Column(name = "motif_desactivation", length = 1000)
    private String motifDesactivation;

    /** Demande maker-checker dont la validation a créé l'élément. */
    @Column(name = "demande_creation_id", updatable = false)
    private UUID demandeCreationId;

    @Column(name = "cree_par", nullable = false, length = 100, updatable = false)
    private String creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    /** Validateur de la création, différent du demandeur. */
    @Column(name = "valide_par", nullable = false, length = 100, updatable = false)
    private String validePar;

    @Column(name = "valide_le", nullable = false, updatable = false)
    private Instant valideLe;

    /** Auteur de la dernière modification validée (libellé, responsable, état). */
    @Column(name = "modifie_par", length = 100)
    private String modifiePar;

    /** Date de validation de la dernière modification. */
    @Column(name = "modifie_le")
    private Instant modifieLe;

    /** Verrou optimiste : deux décisions concurrentes sur le même élément ne s'écrasent pas. */
    @Version
    @Column(nullable = false)
    private Long version;
}
