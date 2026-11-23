package com.sodepa.erp.budgetaire.source.infrastructure.entities;

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
 * Source de financement : l'origine des fonds d'une ligne (F-BUD-026,
 * US-BUD-108).
 *
 * <p>
 * Un financement extérieur n'a pas de budget distinct (H-30) : la source
 * portée par la ligne suffit à suivre ce que chaque partenaire finance. Le code
 * et le type ne changent plus après la création. Le schéma est porté par le
 * changelog 30.0.
 * </p>
 */
@Entity
@Table(name = "budget_source_financement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceFinancementEntity {

    /** Identifiant, dérivé du code (voir {@code SourceFinancementAdapter}). */
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TypeSourceFinancement type;

    /** Partenaire ou bailleur ; obligatoire pour une source de type partenaire. */
    @Column(length = 255)
    private String bailleur;

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
