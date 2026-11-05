package com.sodepa.erp.paie.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "paie_bulletins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinPaieEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periode_id", nullable = false)
    private PeriodePaieEntity periode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    @Column(nullable = false, length = 30)
    private String matricule;

    @Column(name = "nom_complet", nullable = false, length = 200)
    private String nomComplet;

    @Column(name = "structure_nom", length = 150)
    private String structureNom;

    @Column(name = "poste_nom", length = 150)
    private String posteNom;

    @Column(nullable = false)
    private Integer categorie;

    @Column(nullable = false, length = 10)
    private String echelon;

    @Column(name = "numero_cnps", length = 50)
    private String numeroCnps;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 30)
    @Builder.Default
    private ModePaiement modePaiement = ModePaiement.VIREMENT;

    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    // Salaires et gains
    @Column(name = "salaire_base", nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "sursalaire", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal sursalaire = BigDecimal.ZERO;

    @Column(name = "total_primes_imposables", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPrimesImposables = BigDecimal.ZERO;

    @Column(name = "total_primes_non_imposables", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPrimesNonImposables = BigDecimal.ZERO;

    @Column(name = "total_heures_sup", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalHeuresSup = BigDecimal.ZERO;

    @Column(name = "salaire_brut_taxable", nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBrutTaxable;

    @Column(name = "salaire_brut_cotisable_cnps", nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBrutCotisableCnps;

    // Retenues salariales
    @Column(name = "retenue_irpp", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueIrpp = BigDecimal.ZERO;

    @Column(name = "retenue_cac", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueCac = BigDecimal.ZERO;

    @Column(name = "retenue_tc", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueTc = BigDecimal.ZERO;

    @Column(name = "retenue_cfc_salariale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueCfcSalariale = BigDecimal.ZERO;

    @Column(name = "retenue_cnps_salariale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueCnpsSalariale = BigDecimal.ZERO;

    @Column(name = "retenue_syndicat", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenueSyndicat = BigDecimal.ZERO;

    @Column(name = "acomptes_avances", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal acomptesAvances = BigDecimal.ZERO;

    @Column(name = "autres_retenues", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal autresRetenues = BigDecimal.ZERO;

    @Column(name = "total_retenues_salariales", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRetenuesSalariales;

    @Column(name = "net_a_payer", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAPayer;

    // Charges patronales
    @Column(name = "charge_cnps_pvid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCnpsPvid = BigDecimal.ZERO;

    @Column(name = "charge_cnps_pf", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCnpsPf = BigDecimal.ZERO;

    @Column(name = "charge_cnps_at", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCnpsAt = BigDecimal.ZERO;

    @Column(name = "charge_cfc_patronale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeCfcPatronale = BigDecimal.ZERO;

    @Column(name = "charge_fne_patronale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargeFnePatronale = BigDecimal.ZERO;

    @Column(name = "total_charges_patronales", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalChargesPatronales;

    @Column(name = "cout_global_employeur", nullable = false, precision = 18, scale = 2)
    private BigDecimal coutGlobalEmployeur;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String statut = "CALCULE";

    @Column(name = "date_calcul")
    private Instant dateCalcul;

    @OneToMany(mappedBy = "bulletin", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<BulletinLigneEntity> lignes = new ArrayList<>();
}
