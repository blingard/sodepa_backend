package com.sodepa.erp.budgetaire.structure.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Structure budgétaire : une unité de l'organigramme qui porte un budget
 * (F-BUD-025, US-BUD-107).
 *
 * <p>
 * La structure ne recopie rien de l'organigramme : code, libellé et place dans
 * l'arbre se lisent sur l'unité. Elle y ajoute ce qui sert à router une
 * dépense : le poste de l'ordonnateur (DG, directeur d'unité) et le service
 * d'engagement (Service du Budget, service administratif et financier de
 * l'unité).
 * </p>
 *
 * <p>
 * L'ordonnateur est un <strong>poste</strong>, pas une personne : le signataire
 * se retrouve par l'affectation active du poste, de sorte qu'une mutation ne
 * demande aucune ressaisie. Le service d'engagement est une <strong>unité</strong>,
 * parce que plusieurs agents y visent et établissent les bons d'engagement.
 * </p>
 *
 * <p>
 * Le périmètre d'une structure est le sous-arbre de son unité, moins les
 * sous-structures qu'il contient : une dépense relève de la structure la plus
 * proche en remontant l'organigramme (US-BUD-115). Le schéma est porté par le
 * changelog 29.0.
 * </p>
 */
@Entity
@Table(name = "budget_structure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StructureBudgetaireEntity {

    /** Identifiant, dérivé de l'unité (voir {@code StructureBudgetaireAdapter}). */
    @Id
    private UUID id;

    /** Unité de l'organigramme déclarée comme structure ; une seule structure par unité. */
    @Column(name = "unite_id", nullable = false, unique = true, updatable = false)
    private UUID uniteId;

    /** Poste de l'ordonnateur, rattaché à l'unité de la structure. */
    @Column(name = "ordonnateur_poste_id", nullable = false)
    private UUID ordonnateurPosteId;

    /** Unité qui vise et établit les bons d'engagement, dans le sous-arbre de la structure. */
    @Column(name = "service_engagement_unite_id", nullable = false)
    private UUID serviceEngagementUniteId;

    /** Une structure désactivée ne reçoit plus de nouvelle ligne budgétaire. */
    @Column(nullable = false)
    private boolean actif;

    /** Motif de la dernière désactivation ; effacé à la réactivation. */
    @Column(name = "motif_desactivation", length = 1000)
    private String motifDesactivation;

    /** Demande maker-checker dont la validation a créé la structure. */
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

    /** Auteur de la dernière modification validée (ordonnateur, service, état). */
    @Column(name = "modifie_par", length = 100)
    private String modifiePar;

    /** Date de validation de la dernière modification. */
    @Column(name = "modifie_le")
    private Instant modifieLe;

    /** Verrou optimiste : deux décisions concurrentes sur la même structure ne s'écrasent pas. */
    @Version
    @Column(nullable = false)
    private Long version;
}
