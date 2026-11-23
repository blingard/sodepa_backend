package com.sodepa.erp.budgetaire.exercice.infrastructure.entities;

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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Exercice budgétaire : l'année à laquelle se rattachent nomenclature, lignes
 * et budget (F-BUD-001).
 *
 * <p>
 * Distinct de l'exercice comptable ({@code compta_exercices}) : celui-ci suit
 * le cycle de la procédure budgétaire, l'autre découpe l'année en périodes.
 * Le schéma est porté par le changelog 27.0.
 * </p>
 *
 * <p>
 * Il n'existe qu'une fois validé par un second utilisateur : on garde donc
 * l'auteur de la demande et son validateur, que la table des demandes
 * maker-checker ne suffit pas à retrouver une fois purgée.
 * </p>
 */
@Entity
@Table(name = "budget_exercice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciceBudgetaireEntity {

    /** Identifiant, fixé dès la demande de création (voir {@code ExerciceBudgetaireAdapter}). */
    @Id
    private UUID id;

    /** Année civile de l'exercice, unique. */
    @Column(nullable = false, unique = true)
    private Integer annee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EtatExercice etat;

    /** Premier jour de l'exercice : le 1er janvier. */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** Dernier jour de l'exercice : le 31 décembre. */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /** Demande maker-checker dont la validation a créé l'exercice. */
    @Column(name = "demande_creation_id")
    private UUID demandeCreationId;

    /** Utilisateur qui a demandé la création. */
    @Column(name = "cree_par", nullable = false, length = 100)
    private String creePar;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    /** Utilisateur qui a validé la création, différent du demandeur. */
    @Column(name = "valide_par", nullable = false, length = 100)
    private String validePar;

    @Column(name = "valide_le", nullable = false)
    private Instant valideLe;

    /**
     * Verrou optimiste : deux transitions d'état concurrentes (approbation,
     * décision exécutoire) ne peuvent pas s'écraser.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}
