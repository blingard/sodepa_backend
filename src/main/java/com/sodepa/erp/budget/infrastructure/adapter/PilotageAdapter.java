package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.outputs.AuditTrailOutput;
import com.sodepa.erp.budget.application.outputs.RunwayReportOutput;
import com.sodepa.erp.budget.application.outputs.TftOhadaReportOutput;
import com.sodepa.erp.budget.infrastructure.entities.AuditTrailEntity;
import com.sodepa.erp.budget.infrastructure.repo.AuditTrailRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.LigneEcritureRepository;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Composant applicatif `PilotageAdapter` du système ERP SODEPA.
 */
@Component
@RequiredArgsConstructor
public class PilotageAdapter {

    private final LigneEcritureRepository ligneEcritureRepository;
    private final AuditTrailRepository auditTrailRepository;

    @Transactional(readOnly = true)
    public TftOhadaReportOutput genererTftOhada(int annee) {
        LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
        LocalDate finAnnee = LocalDate.of(annee, 12, 31);

        BigDecimal fto = BigDecimal.ZERO;
        BigDecimal fti = BigDecimal.ZERO;
        BigDecimal ftf = BigDecimal.ZERO;
        BigDecimal tresoDebut = BigDecimal.ZERO;
        BigDecimal tresoAnnee = BigDecimal.ZERO;

        for (Object[] row : ligneEcritureRepository.sumMovementsBeforeDateForStatut(StatutEcriture.VALIDE, debutAnnee)) {
            String compte = (String) row[0];
            if (compte != null && compte.startsWith("5")) {
                BigDecimal debit = (BigDecimal) row[1];
                BigDecimal credit = (BigDecimal) row[2];
                tresoDebut = tresoDebut.add(debit.subtract(credit));
            }
        }

        for (Object[] row : ligneEcritureRepository.sumMovementsBetweenDatesForStatut(StatutEcriture.VALIDE, debutAnnee, finAnnee)) {
            String compte = (String) row[0];
            if (compte == null) {
                continue;
            }
            BigDecimal debit = (BigDecimal) row[1];
            BigDecimal credit = (BigDecimal) row[2];
            BigDecimal variationSolde = debit.subtract(credit);

            if (compte.startsWith("5")) {
                tresoAnnee = tresoAnnee.add(variationSolde);
            }
            if (compte.startsWith("2")) {
                fti = fti.subtract(variationSolde);
            } else if (compte.startsWith("16") || compte.startsWith("10")) {
                ftf = ftf.add(variationSolde.negate());
            } else if (compte.startsWith("6") || compte.startsWith("7") || compte.startsWith("4")) {
                fto = fto.add(variationSolde.negate());
            }
        }

        BigDecimal tresoFin = tresoDebut.add(tresoAnnee);
        BigDecimal varNet = fto.add(fti).add(ftf);
        if (tresoDebut.compareTo(BigDecimal.ZERO) == 0 && tresoFin.compareTo(BigDecimal.ZERO) != 0) {
            tresoDebut = tresoFin.subtract(varNet);
        }

        return new TftOhadaReportOutput(annee, fto, fti, ftf, varNet, tresoDebut, tresoFin);
    }

    @Transactional(readOnly = true)
    public RunwayReportOutput calculerRunwayAndBurnRate() {
        LocalDate fin = LocalDate.now();
        LocalDate debut3Mois = fin.minusMonths(3);

        BigDecimal soldeTreso = BigDecimal.ZERO;
        BigDecimal cumulSorties3Mois = BigDecimal.ZERO;

        for (Object[] row : ligneEcritureRepository.sumMovementsByCompteCodeForStatut(StatutEcriture.VALIDE)) {
            String compte = (String) row[0];
            if (compte != null && compte.startsWith("5")) {
                BigDecimal debit = (BigDecimal) row[1];
                BigDecimal credit = (BigDecimal) row[2];
                soldeTreso = soldeTreso.add(debit.subtract(credit));
            }
        }

        for (Object[] row : ligneEcritureRepository.sumMovementsBetweenDatesForStatut(StatutEcriture.VALIDE, debut3Mois, fin)) {
            String compte = (String) row[0];
            if (compte != null && compte.startsWith("5")) {
                BigDecimal credit = (BigDecimal) row[2];
                if (credit.compareTo(BigDecimal.ZERO) > 0) {
                    cumulSorties3Mois = cumulSorties3Mois.add(credit);
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
