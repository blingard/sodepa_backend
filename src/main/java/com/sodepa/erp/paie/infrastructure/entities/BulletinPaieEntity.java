package com.sodepa.erp.paie.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité de persistance représentant `BulletinPaieEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "paie_bulletins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinPaieEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ periode : periode utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periode_id", nullable = false)
    private PeriodePaieEntity periode;

    /**
     * Employé ou collaborateur SODEPA concerné dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    /**
     * Matricule officiel de l'employé SODEPA dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 30)
    private String matricule;

    /**
     * Champ nomComplet : nom complet utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "nom_complet", nullable = false, length = 200)
    private String nomComplet;

    /**
     * Nom ou désignation textuelle de structure pour l'affichage et la traçabilité.
     */
    @Column(name = "structure_nom", length = 150)
    private String structureNom;

    /**
     * Nom ou désignation textuelle de poste pour l'affichage et la traçabilité.
     */
    @Column(name = "poste_nom", length = 150)
    private String posteNom;

    /**
     * Catégorie de rattachement administratif ou technique dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    private Integer categorie;

    /**
     * Champ echelon : echelon utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(nullable = false, length = 10)
    private String echelon;

    /**
     * Champ numeroCnps : numero cnps utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "numero_cnps", length = 50)
    private String numeroCnps;

    /**
     * Champ modePaiement : mode paiement utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 30)
    @Builder.Default
    private ModePaiement modePaiement = ModePaiement.VIREMENT;

    /**
     * Nom ou désignation textuelle de banque pour l'affichage et la traçabilité.
     */
    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    /**
     * Champ compteBancaireRib : compte bancaire rib utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    // Salaires et gains
    /**
     * Champ salaireBase : salaire base utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "salaire_base", nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBase;

    /**
     * Champ sursalaire : sursalaire utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "sursalaire", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal sursalaire = BigDecimal.ZERO;

    /**
     * Champ totalPrimesImposables : total primes imposables utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "total_primes_imposables", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPrimesImposables = BigDecimal.ZERO;

    /**
     * Champ totalPrimesNonImposables : total primes non imposables utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "total_primes_non_imposables", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPrimesNonImposables = BigDecimal.ZERO;

    /**
     * Champ totalHeuresSup : total heures sup utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "total_heures_sup", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalHeuresSup = BigDecimal.ZERO;

    /**
     * Champ salaireBrutTaxable : salaire brut taxable utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "salaire_brut_taxable", nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBrutTaxable;

    /**
     * Champ salaireBrutCotisableCnps : salaire brut cotisable cnps utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "salaire_brut_cotisable_cnps", nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBrutCotisableCnps;

    // Retenues salariales
    /**
     * Champ retenueIrpp : retenue irpp utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "retenue_irpp", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueIrpp = BigDecimal.ZERO;

    /**
     * Champ retenueCac : retenue cac utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "retenue_cac", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueCac = BigDecimal.ZERO;

    /**
     * Champ retenueTc : retenue tc utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "retenue_tc", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueTc = BigDecimal.ZERO;

    /**
     * Champ retenueCfcSalariale : retenue cfc salariale utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "retenue_cfc_salariale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueCfcSalariale = BigDecimal.ZERO;

    /**
     * Champ retenueCnpsSalariale : retenue cnps salariale utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "retenue_cnps_salariale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueCnpsSalariale = BigDecimal.ZERO;

    /**
     * Champ retenueSyndicat : retenue syndicat utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "retenue_syndicat", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueSyndicat = BigDecimal.ZERO;

    /**
     * Champ acomptesAvances : acomptes avances utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "acomptes_avances", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal acomptesAvances = BigDecimal.ZERO;

    /**
     * Champ autresRetenues : autres retenues utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "autres_retenues", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal autresRetenues = BigDecimal.ZERO;

    /**
     * Champ totalRetenuesSalariales : total retenues salariales utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "total_retenues_salariales", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRetenuesSalariales;

    /**
     * Champ netAPayer : net a payer utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "net_a_payer", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAPayer;

    // Charges patronales
    /**
     * Identifiant unique technique (UUID) de l'élément chargecnpspv associé.
     */
    @Column(name = "charge_cnps_pvid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCnpsPvid = BigDecimal.ZERO;

    /**
     * Champ chargeCnpsPf : charge cnps pf utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "charge_cnps_pf", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCnpsPf = BigDecimal.ZERO;

    /**
     * Champ chargeCnpsAt : charge cnps at utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "charge_cnps_at", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCnpsAt = BigDecimal.ZERO;

    /**
     * Champ chargeCfcPatronale : charge cfc patronale utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "charge_cfc_patronale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCfcPatronale = BigDecimal.ZERO;

    /**
     * Champ chargeFnePatronale : charge fne patronale utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "charge_fne_patronale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeFnePatronale = BigDecimal.ZERO;

    /**
     * Champ totalChargesPatronales : total charges patronales utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "total_charges_patronales", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalChargesPatronales;

    /**
     * Valeur financière (cout global employeur) exprimée en Francs CFA (XAF).
     */
    @Column(name = "cout_global_employeur", nullable = false, precision = 18, scale = 2)
    private BigDecimal coutGlobalEmployeur;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String statut = "CALCULE";

    /**
     * Champ dateCalcul : date calcul utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @Column(name = "date_calcul")
    private Instant dateCalcul;

    /**
     * Champ lignes : lignes utilisé pour le pilotage et le suivi dans BulletinPaieEntity.
     */
    @OneToMany(mappedBy = "bulletin", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<BulletinLigneEntity> lignes = new ArrayList<>();
}
