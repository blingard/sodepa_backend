package com.sodepa.erp.tresorerie.regies.presentation.requests;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TypeUniteRegie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Requête de création et paramétrage d'une caisse d'unité / régie par le CSFR (CA-REF-019-01).
 */
@Data
public class CreerRegieRequest {

    /**
     * Code mnémonique unique normalisé.
     */
    @NotBlank
    private String code;

    /**
     * Nom dans le contexte du traitement CreerRegieRequest.
     */
    @NotBlank
    private String nom;

    /**
     * Unité organisationnelle (Direction, Ranch, Abattoir, Service).
     */
    @NotNull
    private TypeUniteRegie typeUnite;

    /**
     * Plafond maximal autorisé en Francs CFA.
     */
    @NotNull
    @Positive
    private BigDecimal plafondAutoriseFcfa;

    /**
     * Regisseur nom dans le contexte du traitement CreerRegieRequest.
     */
    @NotBlank
    private String regisseurNom;

    /**
     * Numéro matricule officiel de l'agent au sein de la SODEPA.
     */
    @NotBlank
    private String regisseurMatricule;

    /**
     * Établissement bancaire domiciliataire.
     */
    private String banqueRattachement;

    /**
     * Relevé d'Identité Bancaire (RIB CEMAC à 23 caractères).
     */
    private String ribCompteRegie;

    /**
     * Compte comptable conforme au plan comptable OHADA révisé.
     */
    @NotBlank
    private String compteComptableCaisse;
}
