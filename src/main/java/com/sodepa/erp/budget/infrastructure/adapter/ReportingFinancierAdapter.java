package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.CreerHorsBilanInput;
import com.sodepa.erp.budget.application.outputs.EngagementHorsBilanOutput;
import com.sodepa.erp.budget.application.outputs.KpiReportOutput;
import com.sodepa.erp.budget.infrastructure.entities.EngagementHorsBilanEntity;
import com.sodepa.erp.budget.infrastructure.repo.EngagementHorsBilanRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.TiersRepository;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportingFinancierAdapter {

    private final EcritureRepository ecritureRepository;
    private final EngagementHorsBilanRepository engagementHorsBilanRepository;
    private final TiersRepository tiersRepository;

    public KpiReportOutput genererRapportKpis() {
        BigDecimal produits = calculerSoldeClasseCompte("7");
        BigDecimal charges = calculerSoldeClasseCompte("6");

        BigDecimal resultatNet = produits.subtract(charges).abs();
        
        BigDecimal capitauxPropres = calculerSoldeClasseCompte("10").abs();
        if (capitauxPropres.compareTo(BigDecimal.ZERO) == 0) {
            capitauxPropres = BigDecimal.valueOf(100000000);
        }

        BigDecimal totalActif = calculerSoldeClasseCompte("2").abs()
                .add(calculerSoldeClasseCompte("3").abs())
                .add(calculerSoldeClasseCompte("5").abs());
        if (totalActif.compareTo(BigDecimal.ZERO) == 0) {
            totalActif = BigDecimal.valueOf(250000000);
        }

        BigDecimal roe = resultatNet.multiply(BigDecimal.valueOf(100)).divide(capitauxPropres, 2, RoundingMode.HALF_UP);
        BigDecimal roa = resultatNet.multiply(BigDecimal.valueOf(100)).divide(totalActif, 2, RoundingMode.HALF_UP);

        BigDecimal actCourtTerme = calculerSoldeClasseCompte("5").abs().add(calculerSoldeClasseCompte("3").abs());
        BigDecimal detCourtTerme = calculerSoldeClasseCompte("4").abs();
        if (detCourtTerme.compareTo(BigDecimal.ZERO) == 0) {
            detCourtTerme = BigDecimal.ONE;
        }
        BigDecimal ratioLiq = actCourtTerme.divide(detCourtTerme, 2, RoundingMode.HALF_UP);

        return new KpiReportOutput(
                resultatNet,
                capitauxPropres,
                totalActif,
                roe,
                roa,
                ratioLiq
        );
    }

    public EngagementHorsBilanOutput enregistrerEngagementHorsBilan(CreerHorsBilanInput input) {
        if (!tiersRepository.existsById(input.tiersId())) {
            throw new IllegalArgumentException("Tiers introuvable avec l'ID: " + input.tiersId());
        }

        EngagementHorsBilanEntity eng = EngagementHorsBilanEntity.builder()
                .type(input.type())
                .intitule(input.intitule())
                .tiersId(input.tiersId())
                .montant(input.montant())
                .dateEffet(input.dateEffet())
                .dateEcheance(input.dateEcheance())
                .statut("ACTIF")
                .build();

        EngagementHorsBilanEntity saved = engagementHorsBilanRepository.save(eng);
        return mapEngagement(saved);
    }

    public List<EngagementHorsBilanOutput> genererReportingHorsBilan() {
        return engagementHorsBilanRepository.findAll().stream()
                .map(this::mapEngagement)
                .collect(Collectors.toList());
    }

    private EngagementHorsBilanOutput mapEngagement(EngagementHorsBilanEntity e) {
        String tiersNom = tiersRepository.findById(e.getTiersId())
                .map(t -> t.getRaisonSociale())
                .orElse("Tiers inconnu");

        return new EngagementHorsBilanOutput(
                e.getId(),
                e.getType(),
                e.getIntitule(),
                e.getTiersId(),
                tiersNom,
                e.getMontant(),
                e.getDateEffet(),
                e.getDateEcheance(),
                e.getStatut()
        );
    }

    private BigDecimal calculerSoldeClasseCompte(String prefix) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<EcritureEntity> ecritures = ecritureRepository.findAll();

        for (EcritureEntity e : ecritures) {
            if (e.getStatut() == StatutEcriture.VALIDE) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    if (l.getCompteCode().startsWith(prefix)) {
                        if (l.getDebit() != null) {
                            totalDebit = totalDebit.add(l.getDebit());
                        }
                        if (l.getCredit() != null) {
                            totalCredit = totalCredit.add(l.getCredit());
                        }
                    }
                }
            }
        }

        return totalDebit.subtract(totalCredit);
    }
}
