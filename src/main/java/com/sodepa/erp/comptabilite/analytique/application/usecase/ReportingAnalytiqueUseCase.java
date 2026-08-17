package com.sodepa.erp.comptabilite.analytique.application.usecase;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.analytique.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.*;
import com.sodepa.erp.utils.StatutEcriture;
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
 * Service applicatif (Use Case) gérant les restitutions et rapports de la comptabilité analytique et budgétaire.
 * Calcule les résultats réels ventilés par section et les compare aux budgets prévisionnels.
 */
@Service
@RequiredArgsConstructor
public class ReportingAnalytiqueUseCase {

    /**
     * Dépôt de données pour les écritures comptables.
     */
    private final EcritureRepository ecritureRepository;

    /**
     * Dépôt de données pour les sections analytiques.
     */
    private final SectionAnalytiqueRepository sectionAnalytiqueRepository;

    /**
     * Dépôt de données pour les budgets prévisionnels.
     */
    private final BudgetRepository budgetRepository;

    /**
     * DTO représentant une section dans le rapport du Grand Livre Analytique.
     */
    @Data
    @Builder
    public static class GrandLivreAnalytiqueSection {
        /**
         * Identifiant unique de la section.
         */
        private UUID sectionId;
        /**
         * Code de la section analytique.
         */
        private String sectionCode;
        /**
         * Libellé ou intitulé de la section.
         */
        private String sectionIntitule;
        /**
         * Liste des imputations détaillées sur cette section.
         */
        private List<LigneAnalytiqueLine> ecritures;
        /**
         * Cumul total des débits affectés à cette section.
         */
        private BigDecimal totalDebit;
        /**
         * Cumul total des crédits affectés à cette section.
         */
        private BigDecimal totalCredit;
        /**
         * Solde net final calculé de la section.
         */
        private BigDecimal soldeNet;
    }

    /**
     * DTO représentant un mouvement détaillé dans le Grand Livre Analytique.
     */
    @Data
    @Builder
    public static class LigneAnalytiqueLine {
        /**
         * Date d'effet comptable de l'écriture générale d'origine.
         */
        private LocalDate dateComptable;
        /**
         * Numéro de pièce comptable générale d'origine.
         */
        private String numeroPiece;
        /**
         * Libellé décrivant la transaction générale d'origine.
         */
        private String libelleEcriture;
        /**
         * Code du compte comptable général affecté (ex: '605200').
         */
        private String compteCode;
        /**
         * Pourcentage de ventilation de cette quote-part.
         */
        private BigDecimal pourcentage;
        /**
         * Montant calculé affecté à cette section (quote-part).
         */
        private BigDecimal montant;
        /**
         * Sens de la ventilation : DEBIT ou CREDIT.
         */
        private String sens;
    }

    /**
     * DTO représentant une ligne de la Balance Analytique.
     */
    @Data
    @Builder
    public static class BalanceAnalytiqueLine {
        /**
         * Nom de l'axe analytique (ex: 'Projet').
         */
        private String axeCode;
        /**
         * Code de la section analytique.
         */
        private String sectionCode;
        /**
         * Libellé descriptif de la section.
         */
        private String sectionIntitule;
        /**
         * Cumul total des débits.
         */
        private BigDecimal cumulDebit;
        /**
         * Cumul total des crédits.
         */
        private BigDecimal cumulCredit;
        /**
         * Solde final calculé.
         */
        private BigDecimal soldeNet;
    }

    /**
     * DTO représentant le Compte de Résultat d'une section analytique.
     */
    @Data
    @Builder
    public static class CompteResultatAnalytiqueReport {
        /**
         * Code de la section analytique.
         */
        private String sectionCode;
        /**
         * Libellé de la section analytique.
         */
        private String sectionIntitule;
        /**
         * Année d'exercice comptable.
         */
        private int annee;
        /**
         * Liste des produits analytiques (classe 7).
         */
        private List<RubriqueResultatAnalytique> produits;
        /**
         * Liste des charges analytiques (classe 6).
         */
        private List<RubriqueResultatAnalytique> charges;
        /**
         * Total cumulé des produits.
         */
        private BigDecimal totalProduits;
        /**
         * Total cumulé des charges.
         */
        private BigDecimal totalCharges;
        /**
         * Résultat net analytique final (totalProduits - totalCharges).
         */
        private BigDecimal resultatNetAnalytique;
    }

    /**
     * DTO représentant une rubrique de charge ou de produit dans le rapport de résultat analytique.
     */
    @Data
    @Builder
    public static class RubriqueResultatAnalytique {
        /**
         * Code du compte comptable général (ex: '601100').
         */
        private String compteCode;
        /**
         * Libellé ou libellé explicite du compte.
         */
        private String compteLibelle;
        /**
         * Montant total imputé (ventilations cumulées).
         */
        private BigDecimal montant;
    }

    /**
     * DTO représentant le suivi budgétaire (réel vs prévisions).
     */
    @Data
    @Builder
    public static class SuiviBudgetaireReport {
        /**
         * Code de la section analytique.
         */
        private String sectionCode;
        /**
         * Libellé de la section.
         */
        private String sectionIntitule;
        /**
         * Année d'exercice budgétaire.
         */
        private int annee;
        /**
         * Lignes comparatives par compte.
         */
        private List<SuiviBudgetaireLine> lignes;
        /**
         * Total budgété cumulé.
         */
        private BigDecimal totalBudget;
        /**
         * Total réel cumulé.
         */
        private BigDecimal totalReel;
        /**
         * Écart net cumulé.
         */
        private BigDecimal ecartNet;
    }

    /**
     * DTO représentant une ligne de comparaison budget vs réel.
     */
    @Data
    @Builder
    public static class SuiviBudgetaireLine {
        /**
         * Code du compte comptable général (ex: '605200').
         */
        private String compteCode;
        /**
         * Montant budgété ou prévisionnel alloué.
         */
        private BigDecimal montantBudget;
        /**
         * Montant réel consommé (ventilations cumulées).
         */
        private BigDecimal montantReel;
        /**
         * Écart de consommation (Reel - Budget).
         */
        private BigDecimal ecart;
        /**
         * Taux d'écart ou de dépassement en pourcentage (ex: +15.5%).
         */
        private BigDecimal pourcentageEcart;
    }

    /**
     * Génère le Grand Livre Analytique sur une période donnée.
     * 
     * @param debut la date de début de période
     * @param fin la date de fin de période
     * @return la liste des sections analytiques avec leurs mouvements détaillés
     */
    @Transactional(readOnly = true)
    public List<GrandLivreAnalytiqueSection> genererGrandLivreAnalytique(LocalDate debut, LocalDate fin) {
        List<SectionAnalytiqueEntity> sections = sectionAnalytiqueRepository.findAll();
        List<EcritureEntity> ecritures = ecritureRepository.findAll().stream()
                .filter(e -> e.getStatut() == StatutEcriture.VALIDE && !e.getDateComptable().isBefore(debut) && !e.getDateComptable().isAfter(fin))
                .collect(Collectors.toList());

        List<GrandLivreAnalytiqueSection> result = new ArrayList<>();

        for (SectionAnalytiqueEntity sec : sections) {
            List<LigneAnalytiqueLine> lignesSection = new ArrayList<>();
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;

            for (EcritureEntity e : ecritures) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    for (VentilationAnalytiqueEntity v : l.getVentilations()) {
                        if (v.getSection().getId().equals(sec.getId())) {
                            String sens = l.getDebit().compareTo(BigDecimal.ZERO) > 0 ? "DEBIT" : "CREDIT";
                            BigDecimal montant = v.getMontant();
                            
                            if ("DEBIT".equals(sens)) {
                                totalDebit = totalDebit.add(montant);
                            } else {
                                totalCredit = totalCredit.add(montant);
                            }

                            lignesSection.add(LigneAnalytiqueLine.builder()
                                    .dateComptable(e.getDateComptable())
                                    .numeroPiece(e.getNumeroPiece())
                                    .libelleEcriture(e.getLibelle())
                                    .compteCode(l.getCompteCode())
                                    .pourcentage(v.getPourcentage())
                                    .montant(montant)
                                    .sens(sens)
                                    .build());
                        }
                    }
                }
            }

            if (!lignesSection.isEmpty()) {
                BigDecimal soldeNet = totalDebit.subtract(totalCredit);
                result.add(GrandLivreAnalytiqueSection.builder()
                        .sectionId(sec.getId())
                        .sectionCode(sec.getCode())
                        .sectionIntitule(sec.getIntitule())
                        .ecritures(lignesSection)
                        .totalDebit(totalDebit)
                        .totalCredit(totalCredit)
                        .soldeNet(soldeNet)
                        .build());
            }
        }

        return result;
    }

    /**
     * Génère la Balance Analytique sur une période donnée.
     * 
     * @param debut la date de début de période
     * @param fin la date de fin de période
     * @return la liste des lignes récapitulatives par section
     */
    @Transactional(readOnly = true)
    public List<BalanceAnalytiqueLine> genererBalanceAnalytique(LocalDate debut, LocalDate fin) {
        List<SectionAnalytiqueEntity> sections = sectionAnalytiqueRepository.findAll();
        List<EcritureEntity> ecritures = ecritureRepository.findAll().stream()
                .filter(e -> e.getStatut() == StatutEcriture.VALIDE && !e.getDateComptable().isBefore(debut) && !e.getDateComptable().isAfter(fin))
                .collect(Collectors.toList());

        List<BalanceAnalytiqueLine> balance = new ArrayList<>();

        for (SectionAnalytiqueEntity sec : sections) {
            BigDecimal cumulDebit = BigDecimal.ZERO;
            BigDecimal cumulCredit = BigDecimal.ZERO;

            for (EcritureEntity e : ecritures) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    for (VentilationAnalytiqueEntity v : l.getVentilations()) {
                        if (v.getSection().getId().equals(sec.getId())) {
                            BigDecimal montant = v.getMontant();
                            if (l.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                                cumulDebit = cumulDebit.add(montant);
                            } else {
                                cumulCredit = cumulCredit.add(montant);
                            }
                        }
                    }
                }
            }

            BigDecimal soldeNet = cumulDebit.subtract(cumulCredit);
            balance.add(BalanceAnalytiqueLine.builder()
                    .axeCode(sec.getAxe().getCode())
                    .sectionCode(sec.getCode())
                    .sectionIntitule(sec.getIntitule())
                    .cumulDebit(cumulDebit)
                    .cumulCredit(cumulCredit)
                    .soldeNet(soldeNet)
                    .build());
        }

        return balance;
    }

    /**
     * Génère le compte de résultat d'une section analytique (Charges vs Produits) pour un exercice.
     * 
     * @param sectionId l'identifiant unique de la section analytique
     * @param annee l'année fiscale concernée
     * @return le rapport de rentabilité de la section
     * @throws IllegalArgumentException si la section n'existe pas
     */
    @Transactional(readOnly = true)
    public CompteResultatAnalytiqueReport genererCompteResultatAnalytique(UUID sectionId, int annee) {
        SectionAnalytiqueEntity section = sectionAnalytiqueRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section analytique introuvable avec l'ID: " + sectionId));

        List<EcritureEntity> ecritures = ecritureRepository.findAll().stream()
                .filter(e -> e.getStatut() == StatutEcriture.VALIDE && e.getDateComptable().getYear() == annee)
                .collect(Collectors.toList());

        Map<String, BigDecimal> cumulCharges = new HashMap<>();
        Map<String, BigDecimal> cumulProduits = new HashMap<>();

        for (EcritureEntity e : ecritures) {
            for (LigneEcritureEntity l : e.getLignes()) {
                for (VentilationAnalytiqueEntity v : l.getVentilations()) {
                    if (v.getSection().getId().equals(sectionId)) {
                        String code = l.getCompteCode();
                        BigDecimal montant = v.getMontant();
                        
                        if (code.startsWith("6")) {
                            BigDecimal ex = cumulCharges.getOrDefault(code, BigDecimal.ZERO);
                            cumulCharges.put(code, ex.add(montant));
                        } else if (code.startsWith("7")) {
                            BigDecimal ex = cumulProduits.getOrDefault(code, BigDecimal.ZERO);
                            cumulProduits.put(code, ex.add(montant));
                        }
                    }
                }
            }
        }

        List<RubriqueResultatAnalytique> chargesList = new ArrayList<>();
        BigDecimal totalCharges = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : cumulCharges.entrySet()) {
            totalCharges = totalCharges.add(entry.getValue());
            chargesList.add(RubriqueResultatAnalytique.builder()
                    .compteCode(entry.getKey())
                    .compteLibelle("Imputation analytique classe 6")
                    .montant(entry.getValue())
                    .build());
        }

        List<RubriqueResultatAnalytique> produitsList = new ArrayList<>();
        BigDecimal totalProduits = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : cumulProduits.entrySet()) {
            totalProduits = totalProduits.add(entry.getValue());
            produitsList.add(RubriqueResultatAnalytique.builder()
                    .compteCode(entry.getKey())
                    .compteLibelle("Imputation analytique classe 7")
                    .montant(entry.getValue())
                    .build());
        }

        BigDecimal net = totalProduits.subtract(totalCharges);

        return CompteResultatAnalytiqueReport.builder()
                .sectionCode(section.getCode())
                .sectionIntitule(section.getIntitule())
                .annee(annee)
                .charges(chargesList)
                .produits(produitsList)
                .totalCharges(totalCharges)
                .totalProduits(totalProduits)
                .resultatNetAnalytique(net)
                .build();
    }

    /**
     * Génère un comparatif Budgets vs Montants Réels (Suivi budgétaire) d'une section analytique.
     * 
     * @param sectionId l'identifiant unique de la section
     * @param annee l'exercice comptable concerné
     * @return le rapport de suivi budgétaire de la section
     * @throws IllegalArgumentException si la section n'existe pas
     */
    @Transactional(readOnly = true)
    public SuiviBudgetaireReport genererSuiviBudgetaire(UUID sectionId, int annee) {
        SectionAnalytiqueEntity section = sectionAnalytiqueRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section analytique introuvable avec l'ID: " + sectionId));

        List<BudgetEntity> budgets = budgetRepository.findByAnneeAndSectionId(annee, sectionId);

        List<EcritureEntity> ecritures = ecritureRepository.findAll().stream()
                .filter(e -> e.getStatut() == StatutEcriture.VALIDE && e.getDateComptable().getYear() == annee)
                .collect(Collectors.toList());

        List<SuiviBudgetaireLine> lignesReport = new ArrayList<>();
        BigDecimal totalBudgetCumul = BigDecimal.ZERO;
        BigDecimal totalReelCumul = BigDecimal.ZERO;

        for (BudgetEntity b : budgets) {
            String compteCode = b.getCompteCode();
            BigDecimal montantBudget = b.getMontantBudget();
            totalBudgetCumul = totalBudgetCumul.add(montantBudget);

            // Calcul du réel (cumul des ventilations de cette section et de ce compte general dans l'année)
            BigDecimal montantReel = BigDecimal.ZERO;
            for (EcritureEntity e : ecritures) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().equals(compteCode)) {
                        for (VentilationAnalytiqueEntity v : l.getVentilations()) {
                            if (v.getSection().getId().equals(sectionId)) {
                                montantReel = montantReel.add(v.getMontant());
                            }
                        }
                    }
                }
            }

            totalReelCumul = totalReelCumul.add(montantReel);
            BigDecimal ecart = montantReel.subtract(montantBudget);
            
            BigDecimal pourcEcart = BigDecimal.ZERO;
            if (montantBudget.compareTo(BigDecimal.ZERO) > 0) {
                pourcEcart = ecart.multiply(BigDecimal.valueOf(100))
                        .divide(montantBudget, 2, RoundingMode.HALF_UP);
            }

            lignesReport.add(SuiviBudgetaireLine.builder()
                    .compteCode(compteCode)
                    .montantBudget(montantBudget)
                    .montantReel(montantReel)
                    .ecart(ecart)
                    .pourcentageEcart(pourcEcart)
                    .build());
        }

        BigDecimal ecartNet = totalReelCumul.subtract(totalBudgetCumul);

        return SuiviBudgetaireReport.builder()
                .sectionCode(section.getCode())
                .sectionIntitule(section.getIntitule())
                .annee(annee)
                .lignes(lignesReport)
                .totalBudget(totalBudgetCumul)
                .totalReel(totalReelCumul)
                .ecartNet(ecartNet)
                .build();
    }
}
