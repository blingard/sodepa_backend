package com.sodepa.erp.budgetaire.nature.infrastructure.entities;

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
 * Nature budgétaire : la classe d'une ligne, avec sa section, son sens et son
 * compte SYSCOHADA (F-BUD-026, US-BUD-108).
 *
 * <p>
 * Plusieurs natures peuvent partager un compte (« Ventes de bovins » et
 * « Ventes de viande » sur le 701). Le code, la section, le sens et le compte
 * ne changent plus après la création : les lignes budgétaires s'en servent.
 * Pour changer de compte, on crée une nouvelle nature et on désactive
 * l'ancienne. Le schéma est porté par le changelog 35.0.
 * </p>
 */
@Entity
@Table(name = "budget_nature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class NatureBudgetaireEntity {

    /** Identifiant, dérivé du code (voir {@code NatureBudgetaireAdapter}). */
    @Id
    private UUID id;

    /** Code en majuscules, unique et jamais réutilisé. */
    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private SectionBudgetaire section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private SensBudgetaire sens;

    /** Compte du plan comptable ({@code comptes}). */
    @Column(name = "compte_id", nullable = false, updatable = false)
    private UUID compteId;

    /** Une nature désactivée ne qualifie plus de nouvelle ligne. */
    @Column(nullable = false)
    private boolean actif;

    @Column(name = "motif_desactivation", length = 1000)
    private String motifDesactivation;

    @Column(name = "demande_creation_id", updatable = false)
    private UUID demandeCreationId;

    @Column(name = "cree_par", nullable = false, length = 100, updatable = false)
    private String creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    @Column(name = "valide_par", nullable = false, length = 100, updatable = false)
    private String validePar;

    @Column(name = "valide_le", nullable = false, updatable = false)
    private Instant valideLe;

    @Column(name = "modifie_par", length = 100)
    private String modifiePar;

    @Column(name = "modifie_le")
    private Instant modifieLe;

    @Version
    @Column(nullable = false)
    private Long version;
}
