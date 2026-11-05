package com.sodepa.erp.paie.application;

import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Moteur de calcul réglementaire de la paie camerounaise et des cotisations sociales CNPS
 * conformément au Code du Travail, Code Général des Impôts (CGI) et textes SODEPA.
 * Rendu 100% configurable à chaud via ParametreConfigurationService.
 */
@Service
@RequiredArgsConstructor
public class PaieCalculateurService {

    private final ParametreConfigurationService configService;

    // Valeurs de repli garanties (Fallback standards SODEPA)
    public static final BigDecimal PLAFOND_MENSUEL_CNPS = new BigDecimal("750000");
    public static final BigDecimal TAUX_CNPS_PVID_SALARIAL = new BigDecimal("0.042"); // 4.2%
    public static final BigDecimal TAUX_CNPS_PVID_PATRONAL = new BigDecimal("0.042"); // 4.2%
    public static final BigDecimal TAUX_CNPS_PF_PATRONAL = new BigDecimal("0.070");   // 7.0%
    public static final BigDecimal TAUX_CNPS_AT_PATRONAL = new BigDecimal("0.025");   // 2.5%
    public static final BigDecimal TAUX_CFC_SALARIAL = new BigDecimal("0.010");       // 1.0%
    public static final BigDecimal TAUX_CFC_PATRONAL = new BigDecimal("0.015");       // 1.5%
    public static final BigDecimal TAUX_FNE_PATRONAL = new BigDecimal("0.010");       // 1.0%
    public static final BigDecimal TAUX_CAC_SUR_IRPP = new BigDecimal("0.100");       // 10% de l'IRPP

    public BigDecimal calculerAssietteCnps(BigDecimal brutCotisable) {
        if (brutCotisable == null || brutCotisable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal plafond = configService.getDecimal("paie.plafond_cnps", PLAFOND_MENSUEL_CNPS);
        return brutCotisable.min(plafond);
    }

    public BigDecimal calculerCnpsSalariale(BigDecimal assietteCnps) {
        BigDecimal taux = configService.getDecimal("paie.taux_cnps_pvid_salarial", TAUX_CNPS_PVID_SALARIAL);
        return arrondir(assietteCnps.multiply(taux));
    }

    public BigDecimal calculerCnpsPatronalePvid(BigDecimal assietteCnps) {
        BigDecimal taux = configService.getDecimal("paie.taux_cnps_pvid_patronal", TAUX_CNPS_PVID_PATRONAL);
        return arrondir(assietteCnps.multiply(taux));
    }

    public BigDecimal calculerCnpsPatronalePf(BigDecimal assietteCnps) {
        BigDecimal taux = configService.getDecimal("paie.taux_cnps_pf_patronal", TAUX_CNPS_PF_PATRONAL);
        return arrondir(assietteCnps.multiply(taux));
    }

    public BigDecimal calculerCnpsPatronaleAt(BigDecimal assietteCnps) {
        BigDecimal taux = configService.getDecimal("paie.taux_cnps_at_patronal", TAUX_CNPS_AT_PATRONAL);
        return arrondir(assietteCnps.multiply(taux));
    }

    public BigDecimal calculerCfcSalariale(BigDecimal brutTaxable) {
        BigDecimal taux = configService.getDecimal("paie.taux_cfc_salarial", TAUX_CFC_SALARIAL);
        return arrondir(brutTaxable.multiply(taux));
    }

    public BigDecimal calculerCfcPatronale(BigDecimal brutTaxable) {
        BigDecimal taux = configService.getDecimal("paie.taux_cfc_patronal", TAUX_CFC_PATRONAL);
        return arrondir(brutTaxable.multiply(taux));
    }

    public BigDecimal calculerFnePatronale(BigDecimal brutTaxable) {
        BigDecimal taux = configService.getDecimal("paie.taux_fne_patronal", TAUX_FNE_PATRONAL);
        return arrondir(brutTaxable.multiply(taux));
    }

    /**
     * Taxe de Développement Local / Taxe Communale (TC) barème mensuel Cameroun.
     */
    public BigDecimal calculerTaxeCommunale(BigDecimal brutMensuel) {
        if (brutMensuel == null || brutMensuel.compareTo(new BigDecimal("62000")) < 0) {
            return BigDecimal.ZERO;
        }
        double val = brutMensuel.doubleValue();
        if (val <= 75000) return new BigDecimal("500");
        if (val <= 100000) return new BigDecimal("1000");
        if (val <= 150000) return new BigDecimal("1500");
        if (val <= 200000) return new BigDecimal("2000");
        if (val <= 250000) return new BigDecimal("2500");
        if (val <= 300000) return new BigDecimal("3000");
        if (val <= 500000) return new BigDecimal("3500");
        return new BigDecimal("4000");
    }

    /**
     * Calcul de l'IRPP mensuel selon le barème progressif officiel du Code Général des Impôts Cameroun.
     * Assiette = Brut taxable - abattement frais professionnels configurable - CNPS salariale.
     */
    public BigDecimal calculerIrpp(BigDecimal brutTaxable, BigDecimal cnpsSalariale) {
        if (brutTaxable == null || brutTaxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal tauxAbattement = configService.getDecimal("paie.taux_abattement_frais_pro", new BigDecimal("0.30"));
        BigDecimal abattement = brutTaxable.multiply(tauxAbattement);
        BigDecimal assietteNet = brutTaxable.subtract(abattement).subtract(cnpsSalariale != null ? cnpsSalariale : BigDecimal.ZERO);

        if (assietteNet.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        double assiette = assietteNet.doubleValue();
        double impot = 0.0;

        // Tranche 1: 0 à 166 666 FCFA (10%)
        double t1Max = 166666.0;
        if (assiette > 0) {
            double part1 = Math.min(assiette, t1Max);
            impot += part1 * 0.10;
        }

        // Tranche 2: 166 667 à 250 000 FCFA (15%)
        double t2Max = 250000.0;
        if (assiette > t1Max) {
            double part2 = Math.min(assiette, t2Max) - t1Max;
            impot += part2 * 0.15;
        }

        // Tranche 3: 250 001 à 416 666 FCFA (25%)
        double t3Max = 416666.0;
        if (assiette > t2Max) {
            double part3 = Math.min(assiette, t3Max) - t2Max;
            impot += part3 * 0.25;
        }

        // Tranche 4: > 416 666 FCFA (35%)
        if (assiette > t3Max) {
            double part4 = assiette - t3Max;
            impot += part4 * 0.35;
        }

        return arrondir(BigDecimal.valueOf(impot));
    }

    public BigDecimal calculerCac(BigDecimal irpp) {
        if (irpp == null || irpp.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tauxCac = configService.getDecimal("paie.taux_cac_sur_irpp", TAUX_CAC_SUR_IRPP);
        return arrondir(irpp.multiply(tauxCac));
    }

    private BigDecimal arrondir(BigDecimal valeur) {
        return valeur.setScale(0, RoundingMode.HALF_UP);
    }
}
