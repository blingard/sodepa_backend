package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.CouvertureInput;
import com.sodepa.erp.budget.application.inputs.CreerPrevisionInput;
import com.sodepa.erp.budget.application.outputs.*;
import com.sodepa.erp.budget.infrastructure.entities.ContratCouvertureEntity;
import com.sodepa.erp.budget.infrastructure.entities.EcheanceFinancementEntity;
import com.sodepa.erp.budget.infrastructure.entities.LigneDecouvertEntity;
import com.sodepa.erp.budget.infrastructure.entities.PrevisionTresorerieEntity;
import com.sodepa.erp.budget.infrastructure.repo.ContratCouvertureRepository;
import com.sodepa.erp.budget.infrastructure.repo.EcheanceFinancementRepository;
import com.sodepa.erp.budget.infrastructure.repo.LigneDecouvertRepository;
import com.sodepa.erp.budget.infrastructure.repo.PrevisionTresorerieRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TresorerieAdapter {

    private final PrevisionTresorerieRepository previsionTresorerieRepository;
    private final LigneDecouvertRepository ligneDecouvertRepository;
    private final EcheanceFinancementRepository echeanceFinancementRepository;
    private final ContratCouvertureRepository contratCouvertureRepository;
    private final EcritureRepository ecritureRepository;

    @Transactional
    public PrevisionTresorerieOutput ajouterPrevision(CreerPrevisionInput input) {
        PrevisionTresorerieEntity prev = PrevisionTresorerieEntity.builder()
                .dateEcheance(input.dateEcheance())
                .type(input.type())
                .source(input.source())
                .libelle(input.libelle())
                .montant(input.montant())
                .build();
        PrevisionTresorerieEntity saved = previsionTresorerieRepository.save(prev);
        return new PrevisionTresorerieOutput(saved.getId(), saved.getDateEcheance(), saved.getType(), saved.getSource(), saved.getLibelle(), saved.getMontant());
    }

    @Transactional(readOnly = true)
    public List<CashFlowMensuelOutput> genererCashFlowPrevisionnel(LocalDate debut, LocalDate fin) {
        List<PrevisionTresorerieEntity> previsions = previsionTresorerieRepository.findByDateEcheanceBetween(debut, fin);
        List<EcheanceFinancementEntity> echeances = echeanceFinancementRepository.findByDateEcheanceBetweenAndStatut(debut, fin, "A_PAYER");

        Map<String, BigDecimal> encaissementsMap = new TreeMap<>();
        Map<String, BigDecimal> decaissementsMap = new TreeMap<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM");

        for (PrevisionTresorerieEntity p : previsions) {
            String moisStr = p.getDateEcheance().format(dtf);
            if ("ENCAISSEMENT".equalsIgnoreCase(p.getType())) {
                encaissementsMap.put(moisStr, encaissementsMap.getOrDefault(moisStr, BigDecimal.ZERO).add(p.getMontant()));
            } else {
                decaissementsMap.put(moisStr, decaissementsMap.getOrDefault(moisStr, BigDecimal.ZERO).add(p.getMontant()));
            }
        }

        for (EcheanceFinancementEntity e : echeances) {
            String moisStr = e.getDateEcheance().format(dtf);
            BigDecimal totalEcheance = e.getPrincipal().add(e.getInterets());
            decaissementsMap.put(moisStr, decaissementsMap.getOrDefault(moisStr, BigDecimal.ZERO).add(totalEcheance));
        }

        List<CashFlowMensuelOutput> cashFlows = new ArrayList<>();
        BigDecimal cumulTresorerie = BigDecimal.ZERO;

        LocalDate courant = debut.withDayOfMonth(1);
        while (!courant.isAfter(fin)) {
            String moisStr = courant.format(dtf);
            BigDecimal enc = encaissementsMap.getOrDefault(moisStr, BigDecimal.ZERO);
            BigDecimal dec = decaissementsMap.getOrDefault(moisStr, BigDecimal.ZERO);
            BigDecimal solde = enc.subtract(dec);
            cumulTresorerie = cumulTresorerie.add(solde);

            cashFlows.add(new CashFlowMensuelOutput(moisStr, enc, dec, solde, cumulTresorerie));

            courant = courant.plusMonths(1);
        }

        return cashFlows;
    }

    @Transactional(readOnly = true)
    public List<OverdraftAlertOutput> verifierSeuilsDecouvert() {
        List<LigneDecouvertEntity> lignes = ligneDecouvertRepository.findAll();
        List<OverdraftAlertOutput> alertes = new ArrayList<>();

        for (LigneDecouvertEntity l : lignes) {
            if (l.getPlafond().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal utilisation = l.getSoldeUtilise()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(l.getPlafond(), 2, RoundingMode.HALF_UP);

                if (utilisation.compareTo(l.getAlerteSeuilPourcent()) >= 0) {
                    alertes.add(new OverdraftAlertOutput(
                            l.getId(),
                            l.getIntitule(),
                            l.getPlafond(),
                            l.getSoldeUtilise(),
                            utilisation,
                            "ATTENTION: L'utilisation du découvert autorisé a atteint " + utilisation 
                                    + "% (Plafond de sécurité: " + l.getAlerteSeuilPourcent() + "%). Risque de liquidité imminent !"
                    ));
                }
            }
        }

        return alertes;
    }

    @Transactional(readOnly = true)
    public BfrReportOutput calculerBFR(LocalDate dateRef) {
        LocalDate fin30j = dateRef.plusDays(30);
        List<PrevisionTresorerieEntity> previsions = previsionTresorerieRepository.findByDateEcheanceBetween(dateRef, fin30j);

        BigDecimal creances = BigDecimal.ZERO;
        BigDecimal dettes = BigDecimal.ZERO;

        for (PrevisionTresorerieEntity p : previsions) {
            if ("ENCAISSEMENT".equalsIgnoreCase(p.getType()) && "CLIENT".equalsIgnoreCase(p.getSource())) {
                creances = creances.add(p.getMontant());
            } else if ("DECAISSEMENT".equalsIgnoreCase(p.getType()) && "FOURNISSEUR".equalsIgnoreCase(p.getSource())) {
                dettes = dettes.add(p.getMontant());
            }
        }

        BigDecimal bfr = creances.subtract(dettes);

        return new BfrReportOutput(dateRef, creances, dettes, bfr);
    }

    @Transactional
    public ContratCouvertureOutput enregistrerCouverture(CouvertureInput input) {
        ContratCouvertureEntity contrat = ContratCouvertureEntity.builder()
                .reference(input.reference())
                .deviseCible(input.devise())
                .montantDevise(input.montantDevise())
                .coursGaranti(input.coursGaranti())
                .dateEffet(input.dateEffet())
                .dateEcheance(input.dateEcheance())
                .statut("ACTIF")
                .build();

        ContratCouvertureEntity c = contratCouvertureRepository.save(contrat);
        return new ContratCouvertureOutput(c.getId(), c.getReference(), c.getDeviseCible(), c.getMontantDevise(), c.getCoursGaranti(), c.getDateEffet(), c.getDateEcheance(), c.getStatut());
    }

    @Transactional(readOnly = true)
    public ValuationCouvertureReportOutput evaluerEcartsDeChangeLatents(UUID contratId, BigDecimal coursSpotActuel) {
        ContratCouvertureEntity contrat = contratCouvertureRepository.findById(contratId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat de couverture de change introuvable."));

        BigDecimal valeurGarantie = contrat.getMontantDevise().multiply(contrat.getCoursGaranti());
        BigDecimal valeurMarche = contrat.getMontantDevise().multiply(coursSpotActuel);
        BigDecimal ecart = valeurMarche.subtract(valeurGarantie);

        String typeEcart = "NEUTRE";
        if (ecart.compareTo(BigDecimal.ZERO) > 0) {
            typeEcart = "GAIN_LATENT";
        } else if (ecart.compareTo(BigDecimal.ZERO) < 0) {
            typeEcart = "PERTE_LATENTE";
        }

        return new ValuationCouvertureReportOutput(
                contrat.getReference(),
                contrat.getDeviseCible(),
                contrat.getMontantDevise(),
                contrat.getCoursGaranti(),
                coursSpotActuel,
                valeurGarantie,
                valeurMarche,
                ecart,
                typeEcart
        );
    }

    @Transactional(readOnly = true)
    public SimulationResultOutput simulerHypotheses(BigDecimal croissance, BigDecimal inflation, BigDecimal varPrixRevient) {
        BigDecimal ventesReelles = calculerCumulComptePrefix("70");
        BigDecimal achatsReels = calculerCumulComptePrefix("60");
        BigDecimal chargesExternes = calculerCumulComptePrefix("61").add(calculerCumulComptePrefix("62"));
        BigDecimal personnelTaxes = calculerCumulComptePrefix("63").add(calculerCumulComptePrefix("64"));

        BigDecimal totalChargesBase = achatsReels.add(chargesExternes).add(personnelTaxes);
        BigDecimal baseEbitda = ventesReelles.subtract(totalChargesBase);

        BigDecimal fCroissance = BigDecimal.ONE.add(croissance.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal fInflation = BigDecimal.ONE.add(inflation.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal fPrixRevient = BigDecimal.ONE.add(varPrixRevient.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        BigDecimal ventesSimulees = ventesReelles.multiply(fCroissance);

        BigDecimal achatsSimules = achatsReels.multiply(fCroissance).multiply(fPrixRevient);
        BigDecimal chargesExternesSimulees = chargesExternes.multiply(fInflation);
        BigDecimal personnelTaxesSimulees = personnelTaxes.multiply(fInflation);

        BigDecimal totalChargesSimulees = achatsSimules.add(chargesExternesSimulees).add(personnelTaxesSimulees);
        BigDecimal simulatedEbitda = ventesSimulees.subtract(totalChargesSimulees);

        BigDecimal baseBfr = ventesReelles.multiply(BigDecimal.valueOf(0.15)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal simulatedBfr = ventesSimulees.multiply(BigDecimal.valueOf(0.15)).setScale(4, RoundingMode.HALF_UP);

        BigDecimal baseCashFlow = baseEbitda;
        BigDecimal simulatedCashFlow = simulatedEbitda;

        return new SimulationResultOutput(
                baseEbitda,
                simulatedEbitda,
                simulatedEbitda.subtract(baseEbitda),
                baseBfr,
                simulatedBfr,
                simulatedBfr.subtract(baseBfr),
                baseCashFlow,
                simulatedCashFlow,
                simulatedCashFlow.subtract(baseCashFlow)
        );
    }

    private BigDecimal calculerCumulComptePrefix(String prefix) {
        BigDecimal total = BigDecimal.ZERO;
        List<EcritureEntity> ecritures = ecritureRepository.findAll();

        for (EcritureEntity e : ecritures) {
            if (e.getStatut() == StatutEcriture.VALIDE) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().startsWith(prefix)) {
                        BigDecimal val = l.getDebit().compareTo(BigDecimal.ZERO) > 0 ? l.getDebit() : l.getCredit();
                        total = total.add(val);
                    }
                }
            }
        }
        return total;
    }
}
