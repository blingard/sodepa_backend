package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.StatutImmobilisation;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Représente une immobilisation d'actif (corporel ou incorporel).
 * Contient les informations nécessaires au calcul des plans d'amortissement (valeur brute, durée de vie, amortissement).
 */
@Entity
@Table(name = "immobilisations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImmobilisationEntity {

    /**
     * Identifiant unique de l'immobilisation (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Code unique d'identification de l'immobilisation (ex: 'IMM-2026-0001').
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Désignation ou description de l'immobilisation.
     */
    @Column(nullable = false, length = 255)
    private String designation;

    /**
     * Valeur brute d'acquisition ou d'apport (valeur d'origine).
     */
    @Column(name = "valeur_origine", nullable = false, precision = 19, scale = 4)
    private BigDecimal valeurOrigine;

    /**
     * Date d'acquisition de l'immobilisation.
     */
    @Column(name = "date_acquisition", nullable = false)
    private LocalDate dateAcquisition;

    /**
     * Date de mise en service effective (début du calcul de l'amortissement).
     */
    @Column(name = "date_mise_en_service", nullable = false)
    private LocalDate dateMiseEnService;

    /**
     * Mode d'amortissement appliqué (ex: LINEAIRE, DEGRESSIF, ACCELERE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_amortissement", nullable = false, length = 50)
    private ModeAmortissement modeAmortissement;

    /**
     * Durée d'utilité estimée en années.
     */
    @Column(name = "duree_utile", nullable = false)
    private Integer dureeUtile;

    /**
     * Valeur résiduelle estimée à la fin de la durée d'utilité (généralement 0).
     */
    @Column(name = "valeur_residuelle", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal valeurResiduelle = BigDecimal.ZERO;

    /**
     * Statut de l'immobilisation (ex: ACTIVE, CEDEE, MISE_AU_REBUT).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutImmobilisation statut;

    /** Position du plan d’amortissement (calcul dynamique). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amortissementCumule = BigDecimal.ZERO;
}
