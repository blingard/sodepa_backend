package com.sodepa.erp.paie.presentation.responses;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DipeDeclarationResponse {

    private UUID periodeId;
    private String periodeCode;
    private Integer mois;
    private Integer annee;
    private String employeurRaisonSociale;
    private String employeurNumeroContribuable;
    private String employeurNumeroCnps;
    private int effectifTotal;

    private BigDecimal totalBrutSalaires;
    private BigDecimal totalCotisableCnps;
    private BigDecimal totalCnpsSalariale;
    private BigDecimal totalCnpsPatronale;
    private BigDecimal totalCnpsGlobal;

    private BigDecimal totalIrpp;
    private BigDecimal totalCac;
    private BigDecimal totalTaxeCommunale;
    private BigDecimal totalCfcSalarial;
    private BigDecimal totalCfcPatronal;
    private BigDecimal totalFnePatronal;
    private BigDecimal totalRetenuesFiscales;

    private List<LigneDipe> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneDipe {
        private String matricule;
        private String numeroCnps;
        private String nom;
        private String prenom;
        private String dateEmbauche;
        private Integer categorie;
        private String echelon;
        private BigDecimal salaireBrut;
        private BigDecimal salairePlafonneCnps;
        private BigDecimal cnpsSalariale;
        private BigDecimal cnpsPatronale;
        private BigDecimal irpp;
        private BigDecimal cac;
        private BigDecimal taxeCommunale;
        private BigDecimal cfcSalarial;
        private BigDecimal netPercu;
    }
}
