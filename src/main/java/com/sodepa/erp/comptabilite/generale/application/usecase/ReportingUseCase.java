package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service applicatif de Reporting et d'Éditions Comptables Réglementaires (SYSCOHADA).
 * 
 * <p><b>Rôle dans le système :</b></p>
 * Fournit les rapports officiels et liasses fiscales : Livre Journal, Grand Livre,
 * Balance Générale (4/6/8 colonnes), Bilan, Compte de Résultat, Tableau des Flux de Trésorerie (TFT)
 * et déclarations de TVA.
 */
@Service
@RequiredArgsConstructor
public class ReportingUseCase {

    private final EcritureRepository ecritureRepository;
    private final CompteRepository compteRepository;

    // ==========================================
    // DTOs POUR LE REPORTING
    // ==========================================

    @Data
    @Builder
    public static class LivreJournalLine {
        private LocalDate dateComptable;
        private String numeroPiece;
        private String libellePiece;
        private String codeJournal;
        private String compteCode;
        private String tiersCode;
        private String libelleLigne;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    @Data
    @Builder
    public static class GrandLivreAccount {
        private String compteCode;
        private String compteIntitule;
        private BigDecimal soldeInitialDebit;
        private BigDecimal soldeInitialCredit;
        private List<GrandLivreLine> ecritures;
        private BigDecimal totalMouvementsDebit;
        private BigDecimal totalMouvementsCredit;
        private BigDecimal soldeFinalDebit;
        private BigDecimal soldeFinalCredit;
    }

    @Data
    @Builder
    public static class GrandLivreLine {
        private LocalDate dateComptable;
        private String numeroPiece;
        private String libellePiece;
        private String libelleLigne;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    @Data
    @Builder
    public static class BalanceLine {
        private String compteCode;
        private String compteIntitule;
        private BigDecimal soldeOuvertureDebit;
        private BigDecimal soldeOuvertureCredit;
        private BigDecimal mouvementsDebit;
        private BigDecimal mouvementsCredit;
        private BigDecimal soldeClotureDebit;
        private BigDecimal soldeClotureCredit;
    }

    @Data
    @Builder
    public static class BilanReport {
        private LocalDate dateBilan;
        private List<BilanLine> actif;
        private List<BilanLine> passif;
        private BigDecimal totalActif;
        private BigDecimal totalPassif;
        private BigDecimal resultatNetBilan;
    }

    @Data
    @Builder
    public static class BilanLine {
        private String categorie;
        private String compteRubrique;
        private BigDecimal montantBrut;
        private BigDecimal amortissements;
        private BigDecimal montantNet;
    }

    @Data
    @Builder
    public static class CompteResultatReport {
        private int annee;
        private BigDecimal chiffreAffaires;
        private BigDecimal consommationsMatieres;
        private BigDecimal servicesExternes;
        private BigDecimal valeurAjoutee;
        private BigDecimal chargesPersonnel;
        private BigDecimal impotsTaxes;
        private BigDecimal excedentBrutExploitation; // EBE
        private BigDecimal dotationsAmortissements;
        private BigDecimal resultatExploitation;
        private BigDecimal resultatFinancier;
        private BigDecimal resultatNet;
    }

    @Data
    @Builder
    public static class TftReport {
        private int annee;
        private BigDecimal fluxOperations;
        private BigDecimal fluxInvestissement;
        private BigDecimal fluxFinancement;
        private BigDecimal variationTresorerie;
    }

    @Data
    @Builder
    public static class TvaDeclaration {
        private int annee;
        private int mois;
        private BigDecimal totalAssietteVentes;
        private BigDecimal tvaCollectee;
        private BigDecimal tvaDeductibleAchats;
        private BigDecimal tvaDeductibleImmo;
        private BigDecimal tvaNetAReverser;
        private BigDecimal creditTvaReportable;
    }

    // ==========================================
    // LOGIQUE DE CALCULS ET TRAITEMENTS
    // ==========================================

    /**
     * Génère le Livre Journal sur une période donnée (chronologique).
     */
    @Transactional(readOnly = true)
    public List<LivreJournalLine> genererLivreJournal(LocalDate debut, LocalDate fin) {
        List<EcritureEntity> ecritures = ecritureRepository.findAll().stream()
                .filter(e -> e.getValide() && !e.getDateComptable().isBefore(debut) && !e.getDateComptable().isAfter(fin))
                .sorted(Comparator.comparing(EcritureEntity::getDateComptable).thenComparing(EcritureEntity::getNumeroPiece))
                .collect(Collectors.toList());

        List<LivreJournalLine> lignesReport = new ArrayList<>();
        for (EcritureEntity e : ecritures) {
            for (LigneEcritureEntity l : e.getLignes()) {
                lignesReport.add(LivreJournalLine.builder()
                        .dateComptable(e.getDateComptable())
                        .numeroPiece(e.getNumeroPiece())
                        .libellePiece(e.getLibelle())
                        .codeJournal(e.getJournal().getCode().name())
                        .compteCode(l.getCompteCode())
                        .tiersCode(l.getTiers() != null ? l.getTiers().getCode() : null)
                        .libelleLigne(l.getLibelleLigne())
                        .debit(l.getDebit())
                        .credit(l.getCredit())
                        .build());
            }
        }
        return lignesReport;
    }

    /**
     * Génère le Grand Livre détaillé par compte sur une période donnée.
     */
    @Transactional(readOnly = true)
    public List<GrandLivreAccount> genererGrandLivre(LocalDate debut, LocalDate fin) {
        List<CompteEntity> comptes = compteRepository.findAll();
        List<EcritureEntity> toutesEcritures = ecritureRepository.findAll();

        // Filtrer les écritures validées avant 'debut' (pour solde initial) et pendant la période
        List<EcritureEntity> ecrituresInitiales = toutesEcritures.stream()
                .filter(e -> e.getValide() && e.getDateComptable().isBefore(debut))
                .collect(Collectors.toList());

        List<EcritureEntity> ecrituresPeriode = toutesEcritures.stream()
                .filter(e -> e.getValide() && !e.getDateComptable().isBefore(debut) && !e.getDateComptable().isAfter(fin))
                .collect(Collectors.toList());

        List<GrandLivreAccount> grandLivre = new ArrayList<>();

        for (CompteEntity c : comptes) {
            // Calcul du solde initial
            BigDecimal soldeInitialDebit = BigDecimal.ZERO;
            BigDecimal soldeInitialCredit = BigDecimal.ZERO;

            BigDecimal cumulInitialDebit = BigDecimal.ZERO;
            BigDecimal cumulInitialCredit = BigDecimal.ZERO;

            for (EcritureEntity e : ecrituresInitiales) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().equals(c.getCode())) {
                        cumulInitialDebit = cumulInitialDebit.add(l.getDebit());
                        cumulInitialCredit = cumulInitialCredit.add(l.getCredit());
                    }
                }
            }

            if (cumulInitialDebit.compareTo(cumulInitialCredit) >= 0) {
                soldeInitialDebit = cumulInitialDebit.subtract(cumulInitialCredit);
            } else {
                soldeInitialCredit = cumulInitialCredit.subtract(cumulInitialDebit);
            }

            // Récupération des lignes de la période
            List<GrandLivreLine> lignesPeriode = new ArrayList<>();
            BigDecimal totalPeriodeDebit = BigDecimal.ZERO;
            BigDecimal totalPeriodeCredit = BigDecimal.ZERO;

            for (EcritureEntity e : ecrituresPeriode) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().equals(c.getCode())) {
                        lignesPeriode.add(GrandLivreLine.builder()
                                .dateComptable(e.getDateComptable())
                                .numeroPiece(e.getNumeroPiece())
                                .libellePiece(e.getLibelle())
                                .libelleLigne(l.getLibelleLigne())
                                .debit(l.getDebit())
                                .credit(l.getCredit())
                                .build());
                        totalPeriodeDebit = totalPeriodeDebit.add(l.getDebit());
                        totalPeriodeCredit = totalPeriodeCredit.add(l.getCredit());
                    }
                }
            }

            // Si aucune écriture et solde initial nul, on n'affiche pas le compte pour alléger le Grand Livre
            if (lignesPeriode.isEmpty() && soldeInitialDebit.compareTo(BigDecimal.ZERO) == 0 && soldeInitialCredit.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Calcul du solde final
            BigDecimal soldeFinalDebit = BigDecimal.ZERO;
            BigDecimal soldeFinalCredit = BigDecimal.ZERO;

            BigDecimal totalDebitCumule = soldeInitialDebit.add(totalPeriodeDebit);
            BigDecimal totalCreditCumule = soldeInitialCredit.add(totalPeriodeCredit);

            if (totalDebitCumule.compareTo(totalCreditCumule) >= 0) {
                soldeFinalDebit = totalDebitCumule.subtract(totalCreditCumule);
            } else {
                soldeFinalCredit = totalCreditCumule.subtract(totalDebitCumule);
            }

            grandLivre.add(GrandLivreAccount.builder()
                    .compteCode(c.getCode())
                    .compteIntitule(c.getIntitule())
                    .soldeInitialDebit(soldeInitialDebit)
                    .soldeInitialCredit(soldeInitialCredit)
                    .ecritures(lignesPeriode)
                    .totalMouvementsDebit(totalPeriodeDebit)
                    .totalMouvementsCredit(totalPeriodeCredit)
                    .soldeFinalDebit(soldeFinalDebit)
                    .soldeFinalCredit(soldeFinalCredit)
                    .build());
        }

        return grandLivre;
    }

    /**
     * Génère la Balance Générale à 6 colonnes.
     */
    @Transactional(readOnly = true)
    public List<BalanceLine> genererBalance(LocalDate debut, LocalDate fin) {
        List<CompteEntity> comptes = compteRepository.findAll();
        List<EcritureEntity> toutesEcritures = ecritureRepository.findAll();

        List<EcritureEntity> ecrituresInitiales = toutesEcritures.stream()
                .filter(e -> e.getValide() && e.getDateComptable().isBefore(debut))
                .collect(Collectors.toList());

        List<EcritureEntity> ecrituresPeriode = toutesEcritures.stream()
                .filter(e -> e.getValide() && !e.getDateComptable().isBefore(debut) && !e.getDateComptable().isAfter(fin))
                .collect(Collectors.toList());

        List<BalanceLine> balance = new ArrayList<>();

        for (CompteEntity c : comptes) {
            BigDecimal cumulInitialDebit = BigDecimal.ZERO;
            BigDecimal cumulInitialCredit = BigDecimal.ZERO;

            for (EcritureEntity e : ecrituresInitiales) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().equals(c.getCode())) {
                        cumulInitialDebit = cumulInitialDebit.add(l.getDebit());
                        cumulInitialCredit = cumulInitialCredit.add(l.getCredit());
                    }
                }
            }

            BigDecimal soldeOuvertureDebit = BigDecimal.ZERO;
            BigDecimal soldeOuvertureCredit = BigDecimal.ZERO;
            if (cumulInitialDebit.compareTo(cumulInitialCredit) >= 0) {
                soldeOuvertureDebit = cumulInitialDebit.subtract(cumulInitialCredit);
            } else {
                soldeOuvertureCredit = cumulInitialCredit.subtract(cumulInitialDebit);
            }

            BigDecimal mouvementsDebit = BigDecimal.ZERO;
            BigDecimal mouvementsCredit = BigDecimal.ZERO;

            for (EcritureEntity e : ecrituresPeriode) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().equals(c.getCode())) {
                        mouvementsDebit = mouvementsDebit.add(l.getDebit());
                        mouvementsCredit = mouvementsCredit.add(l.getCredit());
                    }
                }
            }

            // Ignorer si aucune activité
            if (soldeOuvertureDebit.compareTo(BigDecimal.ZERO) == 0 &&
                soldeOuvertureCredit.compareTo(BigDecimal.ZERO) == 0 &&
                mouvementsDebit.compareTo(BigDecimal.ZERO) == 0 &&
                mouvementsCredit.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal soldeClotureDebit = BigDecimal.ZERO;
            BigDecimal soldeClotureCredit = BigDecimal.ZERO;
            BigDecimal totalDebitCumule = soldeOuvertureDebit.add(mouvementsDebit);
            BigDecimal totalCreditCumule = soldeOuvertureCredit.add(mouvementsCredit);

            if (totalDebitCumule.compareTo(totalCreditCumule) >= 0) {
                soldeClotureDebit = totalDebitCumule.subtract(totalCreditCumule);
            } else {
                soldeClotureCredit = totalCreditCumule.subtract(totalDebitCumule);
            }

            balance.add(BalanceLine.builder()
                    .compteCode(c.getCode())
                    .compteIntitule(c.getIntitule())
                    .soldeOuvertureDebit(soldeOuvertureDebit)
                    .soldeOuvertureCredit(soldeOuvertureCredit)
                    .mouvementsDebit(mouvementsDebit)
                    .mouvementsCredit(mouvementsCredit)
                    .soldeClotureDebit(soldeClotureDebit)
                    .soldeClotureCredit(soldeClotureCredit)
                    .build());
        }

        return balance;
    }

    /**
     * Génère le Bilan comptable (Actif et Passif) à une date donnée.
     */
    @Transactional(readOnly = true)
    public BilanReport genererBilan(LocalDate dateBilan) {
        List<BalanceLine> balance = genererBalance(LocalDate.of(dateBilan.getYear(), 1, 1), dateBilan);

        List<BilanLine> actif = new ArrayList<>();
        List<BilanLine> passif = new ArrayList<>();

        BigDecimal totalActif = BigDecimal.ZERO;
        BigDecimal totalPassif = BigDecimal.ZERO;
        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalProduits = BigDecimal.ZERO;

        for (BalanceLine l : balance) {
            String code = l.getCompteCode();
            BigDecimal soldeNet = l.getSoldeClotureDebit().subtract(l.getSoldeClotureCredit());

            // 1. Calcul du résultat net intermédiaire (Classes 6 et 7) pour équilibrer le bilan
            if (code.startsWith("6")) {
                totalCharges = totalCharges.add(soldeNet);
                continue;
            }
            if (code.startsWith("7")) {
                totalProduits = totalProduits.add(soldeNet.negate()); // les produits ont un solde créditeur
                continue;
            }

            // 2. Ventilation Actif / Passif (Classes 1 à 5)
            if (code.startsWith("2")) { // Immobilisations
                BigDecimal brut = l.getSoldeClotureDebit();
                BigDecimal amort = l.getSoldeClotureCredit(); // Dans le plan, amortissements = crédit 28x
                BigDecimal net = brut.subtract(amort);

                actif.add(BilanLine.builder()
                        .categorie("ACTIF IMMOBILISÉ")
                        .compteRubrique(code + " - " + l.getCompteIntitule())
                        .montantBrut(brut)
                        .amortissements(amort)
                        .montantNet(net)
                        .build());
                totalActif = totalActif.add(net);
            } else if (code.startsWith("3")) { // Stocks
                actif.add(BilanLine.builder()
                        .categorie("ACTIF CIRCULANT (STOCKS)")
                        .compteRubrique(code + " - " + l.getCompteIntitule())
                        .montantBrut(soldeNet)
                        .amortissements(BigDecimal.ZERO)
                        .montantNet(soldeNet)
                        .build());
                totalActif = totalActif.add(soldeNet);
            } else if (code.startsWith("4")) { // Tiers et régularisations
                if (soldeNet.compareTo(BigDecimal.ZERO) >= 0) { // Débiteur -> Créances Actif
                    actif.add(BilanLine.builder()
                            .categorie("CRÉANCES ACTIF")
                            .compteRubrique(code + " - " + l.getCompteIntitule())
                            .montantBrut(soldeNet)
                            .amortissements(BigDecimal.ZERO)
                            .montantNet(soldeNet)
                            .build());
                    totalActif = totalActif.add(soldeNet);
                } else { // Créditeur -> Dettes Passif
                    BigDecimal dette = soldeNet.negate();
                    passif.add(BilanLine.builder()
                            .categorie("DETTES CIRCULANTES")
                            .compteRubrique(code + " - " + l.getCompteIntitule())
                            .montantBrut(dette)
                            .amortissements(BigDecimal.ZERO)
                            .montantNet(dette)
                            .build());
                    totalPassif = totalPassif.add(dette);
                }
            } else if (code.startsWith("5")) { // Trésorerie
                if (soldeNet.compareTo(BigDecimal.ZERO) >= 0) { // Banque / Caisse Actif
                    actif.add(BilanLine.builder()
                            .categorie("TRÉSORERIE ACTIF")
                            .compteRubrique(code + " - " + l.getCompteIntitule())
                            .montantBrut(soldeNet)
                            .amortissements(BigDecimal.ZERO)
                            .montantNet(soldeNet)
                            .build());
                    totalActif = totalActif.add(soldeNet);
                } else { // Découverts Passif
                    BigDecimal decouvert = soldeNet.negate();
                    passif.add(BilanLine.builder()
                            .categorie("TRÉSORERIE PASSIF")
                            .compteRubrique(code + " - " + l.getCompteIntitule())
                            .montantBrut(decouvert)
                            .amortissements(BigDecimal.ZERO)
                            .montantNet(decouvert)
                            .build());
                    totalPassif = totalPassif.add(decouvert);
                }
            } else if (code.startsWith("1")) { // Capitaux propres et emprunts
                BigDecimal montant = soldeNet.negate(); // Créditeur
                String cat = code.startsWith("16") ? "DETTES FINANCIÈRES" : "CAPITAUX PROPRES";
                passif.add(BilanLine.builder()
                        .categorie(cat)
                        .compteRubrique(code + " - " + l.getCompteIntitule())
                        .montantBrut(montant)
                        .amortissements(BigDecimal.ZERO)
                        .montantNet(montant)
                        .build());
                totalPassif = totalPassif.add(montant);
            }
        }

        // Intégration du résultat de l'exercice au Passif (Bénéfice ou Perte)
        BigDecimal resultatNet = totalProduits.subtract(totalCharges);
        passif.add(BilanLine.builder()
                .categorie("CAPITAUX PROPRES")
                .compteRubrique("131/139 - Résultat Net de l'Exercice")
                .montantBrut(resultatNet)
                .amortissements(BigDecimal.ZERO)
                .montantNet(resultatNet)
                .build());
        totalPassif = totalPassif.add(resultatNet);

        return BilanReport.builder()
                .dateBilan(dateBilan)
                .actif(actif)
                .passif(passif)
                .totalActif(totalActif)
                .totalPassif(totalPassif)
                .resultatNetBilan(resultatNet)
                .build();
    }

    /**
     * Génère le Compte de Résultat (Marge, Valeur Ajoutée, EBE, Résultat Net) pour un exercice.
     */
    @Transactional(readOnly = true)
    public CompteResultatReport genererCompteResultat(int annee) {
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);
        List<BalanceLine> balance = genererBalance(debut, fin);

        BigDecimal chiffreAffaires = BigDecimal.ZERO;
        BigDecimal consommationsMatieres = BigDecimal.ZERO;
        BigDecimal servicesExternes = BigDecimal.ZERO;
        BigDecimal chargesPersonnel = BigDecimal.ZERO;
        BigDecimal impotsTaxes = BigDecimal.ZERO;
        BigDecimal dotationsAmortissements = BigDecimal.ZERO;
        BigDecimal produitsFinanciers = BigDecimal.ZERO;
        BigDecimal chargesFinancieres = BigDecimal.ZERO;
        BigDecimal impotsResultat = BigDecimal.ZERO;

        for (BalanceLine l : balance) {
            String code = l.getCompteCode();
            BigDecimal soldeNet = l.getSoldeClotureDebit().subtract(l.getSoldeClotureCredit());

            if (code.startsWith("70")) { // Ventes
                chiffreAffaires = chiffreAffaires.add(soldeNet.negate());
            } else if (code.startsWith("60")) { // Consommations d'achats
                consommationsMatieres = consommationsMatieres.add(soldeNet);
            } else if (code.startsWith("61") || code.startsWith("62") || code.startsWith("63")) { // Services ext.
                servicesExternes = servicesExternes.add(soldeNet);
            } else if (code.startsWith("66")) { // Charges de personnel
                chargesPersonnel = chargesPersonnel.add(soldeNet);
            } else if (code.startsWith("64")) { // Impôts & taxes
                impotsTaxes = impotsTaxes.add(soldeNet);
            } else if (code.startsWith("68")) { // Dotations aux amortissements
                dotationsAmortissements = dotationsAmortissements.add(soldeNet);
            } else if (code.startsWith("77")) { // Produits financiers
                produitsFinanciers = produitsFinanciers.add(soldeNet.negate());
            } else if (code.startsWith("67")) { // Charges financières
                chargesFinancieres = chargesFinancieres.add(soldeNet);
            } else if (code.startsWith("89")) { // Impôt sur le résultat
                impotsResultat = impotsResultat.add(soldeNet);
            }
        }

        // Calcul des Soldes Intermédiaires de Gestion
        BigDecimal valeurAjoutee = chiffreAffaires
                .subtract(consommationsMatieres)
                .subtract(servicesExternes);

        BigDecimal excedentBrutExploitation = valeurAjoutee
                .subtract(impotsTaxes)
                .subtract(chargesPersonnel);

        BigDecimal resultatExploitation = excedentBrutExploitation
                .subtract(dotationsAmortissements);

        BigDecimal resultatFinancier = produitsFinanciers
                .subtract(chargesFinancieres);

        BigDecimal resultatNet = resultatExploitation
                .add(resultatFinancier)
                .subtract(impotsResultat);

        return CompteResultatReport.builder()
                .annee(annee)
                .chiffreAffaires(chiffreAffaires)
                .consommationsMatieres(consommationsMatieres)
                .servicesExternes(servicesExternes)
                .valeurAjoutee(valeurAjoutee)
                .chargesPersonnel(chargesPersonnel)
                .impotsTaxes(impotsTaxes)
                .excedentBrutExploitation(excedentBrutExploitation)
                .dotationsAmortissements(dotationsAmortissements)
                .resultatExploitation(resultatExploitation)
                .resultatFinancier(resultatFinancier)
                .resultatNet(resultatNet)
                .build();
    }

    /**
     * Génère le Tableau des Flux de Trésorerie (TFT).
     */
    @Transactional(readOnly = true)
    public TftReport genererTft(int annee) {
        CompteResultatReport res = genererCompteResultat(annee);
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);
        List<BalanceLine> balance = genererBalance(debut, fin);

        // 1. Flux Opérationnels = Résultat Net + Amortissements - Variation du BFR
        BigDecimal varStocks = BigDecimal.ZERO;
        BigDecimal varCreances = BigDecimal.ZERO;
        BigDecimal varDettes = BigDecimal.ZERO;

        for (BalanceLine l : balance) {
            String code = l.getCompteCode();
            BigDecimal variation = l.getMouvementsDebit().subtract(l.getMouvementsCredit());

            if (code.startsWith("3")) { // Stocks
                varStocks = varStocks.add(variation);
            } else if (code.startsWith("41")) { // Créances Clients
                varCreances = varCreances.add(variation);
            } else if (code.startsWith("40")) { // Dettes Fournisseurs
                varDettes = varDettes.add(variation);
            }
        }

        // BFR Variation = + variation stocks + variation créances - variation dettes
        BigDecimal variationBfr = varStocks.add(varCreances).subtract(varDettes);
        BigDecimal fluxOperations = res.getResultatNet()
                .add(res.getDotationsAmortissements())
                .subtract(variationBfr);

        // 2. Flux d'Investissement = Cash dépensé pour les immobilisations (Classe 2 acquisitions)
        BigDecimal fluxInvestissement = BigDecimal.ZERO;
        for (BalanceLine l : balance) {
            if (l.getCompteCode().startsWith("2") && !l.getCompteCode().startsWith("28")) {
                // Mouvements Débit d'actifs = acquisitions
                fluxInvestissement = fluxInvestissement.add(l.getMouvementsDebit());
            }
        }
        fluxInvestissement = fluxInvestissement.negate(); // Sortie de cash

        // 3. Flux de Financement = Variation du capital social et des emprunts à long terme
        BigDecimal fluxFinancement = BigDecimal.ZERO;
        for (BalanceLine l : balance) {
            if (l.getCompteCode().startsWith("10") || l.getCompteCode().startsWith("16")) {
                // Variation au crédit = apport de cash (emprunt/capital)
                BigDecimal variation = l.getMouvementsCredit().subtract(l.getMouvementsDebit());
                fluxFinancement = fluxFinancement.add(variation);
            }
        }

        BigDecimal variationTresorerie = fluxOperations.add(fluxInvestissement).add(fluxFinancement);

        return TftReport.builder()
                .annee(annee)
                .fluxOperations(fluxOperations)
                .fluxInvestissement(fluxInvestissement)
                .fluxFinancement(fluxFinancement)
                .variationTresorerie(variationTresorerie)
                .build();
    }

    /**
     * Génère la Déclaration Périodique de TVA (mensuelle ou trimestrielle).
     */
    @Transactional(readOnly = true)
    public TvaDeclaration genererDeclarationTva(int annee, int mois) {
        LocalDate debut = LocalDate.of(annee, mois, 1);
        LocalDate fin = debut.withDayOfMonth(debut.lengthOfMonth());
        List<BalanceLine> balance = genererBalance(debut, fin);

        BigDecimal totalAssietteVentes = BigDecimal.ZERO;
        BigDecimal tvaCollectee = BigDecimal.ZERO;
        BigDecimal tvaDeductibleAchats = BigDecimal.ZERO;
        BigDecimal tvaDeductibleImmo = BigDecimal.ZERO;

        for (BalanceLine l : balance) {
            String code = l.getCompteCode();
            BigDecimal netMouvements = l.getMouvementsCredit().subtract(l.getMouvementsDebit());

            if (code.startsWith("70")) { // Base d'imposition (Ventes HT)
                totalAssietteVentes = totalAssietteVentes.add(netMouvements);
            } else if (code.startsWith("443")) { // TVA Collectée (créditée lors des ventes)
                tvaCollectee = tvaCollectee.add(netMouvements);
            } else if (code.startsWith("4452")) { // TVA déductible sur achats (débitée)
                tvaDeductibleAchats = tvaDeductibleAchats.add(l.getMouvementsDebit().subtract(l.getMouvementsCredit()));
            } else if (code.startsWith("4451")) { // TVA déductible sur immobilisations (débitée)
                tvaDeductibleImmo = tvaDeductibleImmo.add(l.getMouvementsDebit().subtract(l.getMouvementsCredit()));
            }
        }

        BigDecimal totalDeductible = tvaDeductibleAchats.add(tvaDeductibleImmo);
        BigDecimal tvaNetAReverser = BigDecimal.ZERO;
        BigDecimal creditTvaReportable = BigDecimal.ZERO;

        if (tvaCollectee.compareTo(totalDeductible) >= 0) {
            tvaNetAReverser = tvaCollectee.subtract(totalDeductible);
        } else {
            creditTvaReportable = totalDeductible.subtract(tvaCollectee);
        }

        return TvaDeclaration.builder()
                .annee(annee)
                .mois(mois)
                .totalAssietteVentes(totalAssietteVentes)
                .tvaCollectee(tvaCollectee)
                .tvaDeductibleAchats(tvaDeductibleAchats)
                .tvaDeductibleImmo(tvaDeductibleImmo)
                .tvaNetAReverser(tvaNetAReverser)
                .creditTvaReportable(creditTvaReportable)
                .build();
    }
}
