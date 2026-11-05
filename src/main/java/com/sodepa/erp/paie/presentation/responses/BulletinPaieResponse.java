package com.sodepa.erp.paie.presentation.responses;

import com.sodepa.erp.paie.infrastructure.entities.ModePaiement;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinPaieResponse {

    private UUID id;
    private UUID periodeId;
    private String periodeCode;
    private UUID employeId;
    private String matricule;
    private String nomComplet;
    private String structureNom;
    private String posteNom;
    private Integer categorie;
    private String echelon;
    private String numeroCnps;
    private ModePaiement modePaiement;
    private String banqueNom;
    private String compteBancaireRib;

    // Gains
    private BigDecimal salaireBase;
    private BigDecimal sursalaire;
    private BigDecimal totalPrimesImposables;
    private BigDecimal totalPrimesNonImposables;
    private BigDecimal totalHeuresSup;
    private BigDecimal salaireBrutTaxable;
    private BigDecimal salaireBrutCotisableCnps;

    // Retenues salariales
    private BigDecimal retenueIrpp;
    private BigDecimal retenueCac;
    private BigDecimal retenueTc;
    private BigDecimal retenueCfcSalariale;
    private BigDecimal retenueCnpsSalariale;
    private BigDecimal retenueSyndicat;
    private BigDecimal acomptesAvances;
    private BigDecimal autresRetenues;
    private BigDecimal totalRetenuesSalariales;
    private BigDecimal netAPayer;

    // Charges patronales
    private BigDecimal chargeCnpsPvid;
    private BigDecimal chargeCnpsPf;
    private BigDecimal chargeCnpsAt;
    private BigDecimal chargeCfcPatronale;
    private BigDecimal chargeFnePatronale;
    private BigDecimal totalChargesPatronales;
    private BigDecimal coutGlobalEmployeur;

    private String statut;
    private Instant dateCalcul;
    private List<BulletinLigneResponse> lignes;
}
