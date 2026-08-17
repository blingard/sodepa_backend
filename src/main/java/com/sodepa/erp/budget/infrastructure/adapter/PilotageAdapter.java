package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.outputs.AuditTrailOutput;
import com.sodepa.erp.budget.application.outputs.RunwayReportOutput;
import com.sodepa.erp.budget.application.outputs.TftOhadaReportOutput;
import com.sodepa.erp.budget.infrastructure.entities.AuditTrailEntity;
import com.sodepa.erp.budget.infrastructure.repo.AuditTrailRepository;
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
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PilotageAdapter {

    private final EcritureRepository ecritureRepository;
    private final AuditTrailRepository auditTrailRepository;

    @Transactional(readOnly = true)
    public TftOhadaReportOutput genererTftOhada(int annee) {
        List<EcritureEntity> ecritures = ecritureRepository.findAll();

        BigDecimal fto = BigDecimal.ZERO;
        BigDecimal fti = BigDecimal.ZERO;
        BigDecimal ftf = BigDecimal.ZERO;
        BigDecimal tresoDebut = BigDecimal.ZERO;
        BigDecimal tresoFin = BigDecimal.ZERO;

        for (EcritureEntity e : ecritures) {
            if (e.getStatut() != StatutEcriture.VALIDE) {
                continue;
            }

            int anneeComptable = e.getDateComptable().getYear();

            for (LigneEcritureEntity l : e.getLignes()) {
                String compte = l.getCompteCode();
                BigDecimal debit = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
                BigDecimal variationSolde = debit.subtract(credit);

                if (compte.startsWith("5")) {
                    if (anneeComptable < annee) {
                        tresoDebut = tresoDebut.add(variationSolde);
                    }
                    if (anneeComptable <= annee) {
                        tresoFin = tresoFin.add(variationSolde);
                    }
                }

                if (anneeComptable == annee) {
                    if (compte.startsWith("2")) {
                        fti = fti.subtract(variationSolde);
                    } else if (compte.startsWith("16") || compte.startsWith("10")) {
                        ftf = ftf.add(variationSolde.negate());
                    } else if (compte.startsWith("6") || compte.startsWith("7") || compte.startsWith("4")) {
                        if (compte.startsWith("7")) {
                            fto = fto.add(credit.subtract(debit));
                        } else if (compte.startsWith("6")) {
                            fto = fto.subtract(debit.subtract(credit));
                        } else {
                            fto = fto.add(variationSolde.negate());
                        }
                    }
                }
            }
        }

        BigDecimal varNet = fto.add(fti).add(ftf);
        if (tresoDebut.compareTo(BigDecimal.ZERO) == 0 && tresoFin.compareTo(BigDecimal.ZERO) != 0) {
            tresoDebut = tresoFin.subtract(varNet);
        }

        return new TftOhadaReportOutput(annee, fto, fti, ftf, varNet, tresoDebut, tresoFin);
    }

    @Transactional(readOnly = true)
    public RunwayReportOutput calculerRunwayAndBurnRate() {
        List<EcritureEntity> ecritures = ecritureRepository.findAll();

        LocalDate fin = LocalDate.now();
        LocalDate debut3Mois = fin.minusMonths(3);

        BigDecimal soldeTreso = BigDecimal.ZERO;
        BigDecimal cumulSorties3Mois = BigDecimal.ZERO;

        for (EcritureEntity e : ecritures) {
            if (e.getStatut() != StatutEcriture.VALIDE) {
                continue;
            }

            for (LigneEcritureEntity l : e.getLignes()) {
                String compte = l.getCompteCode();
                BigDecimal debit = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
                BigDecimal variation = debit.subtract(credit);

                if (compte.startsWith("5")) {
                    soldeTreso = soldeTreso.add(variation);

                    if (!e.getDateComptable().isBefore(debut3Mois) && !e.getDateComptable().isAfter(fin)) {
                        if (credit.compareTo(BigDecimal.ZERO) > 0) {
                            cumulSorties3Mois = cumulSorties3Mois.add(credit);
                        }
                    }
                }
            }
        }

        BigDecimal burnRate = cumulSorties3Mois.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
        
        if (burnRate.compareTo(BigDecimal.ZERO) <= 0) {
            burnRate = BigDecimal.valueOf(1000000);
        }

        BigDecimal runway = soldeTreso.divide(burnRate, 2, RoundingMode.HALF_UP);
        if (runway.compareTo(BigDecimal.ZERO) < 0) {
            runway = BigDecimal.ZERO;
        }

        String diagnostic = "SITUATION DE LIQUIDITÉ SAINE";
        if (runway.compareTo(BigDecimal.valueOf(3)) < 0) {
            diagnostic = "ALERTE CRITIQUE : Autonomie de trésorerie inférieure à 3 mois ! Urgence de refinancement ou de réduction des coûts.";
        } else if (runway.compareTo(BigDecimal.valueOf(6)) < 0) {
            diagnostic = "VIGILANCE RECOMMANDÉE : Autonomie modérée. Planifier des rentrées de fonds ou surveiller les charges.";
        }

        return new RunwayReportOutput(soldeTreso, burnRate, runway, diagnostic);
    }

    @Transactional(readOnly = true)
    public List<AuditTrailOutput> consulterAuditTrail(String entiteNom, UUID entiteId) {
        List<AuditTrailEntity> logs = auditTrailRepository.findByEntiteNomAndEntiteIdOrderByTimestampDesc(entiteNom, entiteId);
        return logs.stream().map(e -> new AuditTrailOutput(
                e.getId(),
                e.getEntiteNom(),
                e.getEntiteId(),
                e.getAction(),
                e.getDetails(),
                e.getTimestamp(),
                e.getUtilisateur()
        )).toList();
    }
}
