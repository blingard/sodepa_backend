package com.sodepa.erp.fiscalite.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Table unique transversale des seuils de gestion SODEPA (F-REF-020 / CA-REF-020-02 / CA-REF-020-03 / PO-REF-02).
 * Unifie les seuils des modules Budget, Achats/Marchés, Trésorerie/Caisses et Comptabilité
 * avec gestion native des dates d'effet (RG-REF-007) et historisation sans écrasement rétroactif.
 */
@Entity
@Table(name = "ref_seuils_gestion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeuilGestionEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, length = 60)
    private String code;

    /**
     * Domaine consommateur : ACHATS, BUDGET, TRESORERIE_CAISSE, COMPTABILITE.
     */
    @Column(nullable = false, length = 40)
    private String domaine;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 180)
    private String libelle;

    /**
     * Montant financier exprimé en Francs CFA (XAF).
     */
    @Column(name = "montant_min_fcfa", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantMinFcfa;

    /**
     * Montant financier exprimé en Francs CFA (XAF).
     */
    @Column(name = "montant_max_fcfa", precision = 18, scale = 2)
    private BigDecimal montantMaxFcfa;

    /**
     * Nb proformas requis dans le contexte du traitement SeuilGestionEntity.
     */
    @Column(name = "nb_proformas_requis", nullable = false)
    @Builder.Default
    private Integer nbProformasRequis = 0;

    /**
     * Indicateur booléen précisant si commission requise est effectif ou validé.
     */
    @Column(name = "commission_requise", nullable = false)
    @Builder.Default
    private boolean commissionRequise = false;

    /**
     * Autorité ou organe d'approbation requis : CHEF_UNITE, CSFR, DAF, DG, CIPM, CA.
     */
    @Column(name = "autorite_validation", nullable = false, length = 60)
    private String autoriteValidation;

    /**
     * Date d'entrée en vigueur du seuil (CA-REF-020-03 / RG-REF-007).
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Date de fin de validité (null si le seuil est actuellement en vigueur sans date d'expiration).
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
