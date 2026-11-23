package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.CreerHorsBilanInput;
import com.sodepa.erp.budget.application.outputs.EngagementHorsBilanOutput;
import com.sodepa.erp.budget.application.outputs.KpiReportOutput;
import com.sodepa.erp.budget.infrastructure.entities.EngagementHorsBilanEntity;
import com.sodepa.erp.budget.infrastructure.repo.EngagementHorsBilanRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.LigneEcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.TiersRepository;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `ReportingFinancierAdapter`.
 */
@Component
@RequiredArgsConstructor
public class ReportingFinancierAdapter {

    private final LigneEcritureRepository ligneEcritureRepository;
    private final EngagementHorsBilanRepository engagementHorsBilanRepository;
    private final TiersRepository tiersRepository;

    public KpiReportOutput genererRapportKpis() {
        List<Object[]> agregats = ligneEcritureRepository.sumMovementsByCompteCodeForStatut(StatutEcriture.VALIDE);

        // SYSCOHADA : Produits (classe 7) sont créditeurs => Crédit - Débit ; Charges (classe 6) sont débiteurs => Débit - Crédit
        BigDecimal produits = calculerSoldeCreditMoinsDebit(agregats, "7");
        BigDecimal charges = calculerSoldeDebitMoinsCredit(agregats, "6");

        // Conservation du signe réel du résultat net (bénéfice >= 0, déficit < 0)
        BigDecimal resultatNet = produits.subtract(charges);

        BigDecimal capitauxPropres = calculerSoldeDebitMoinsCredit(agregats, "10").abs();
        if (capitauxPropres.compareTo(BigDecimal.ZERO) == 0) {
            capitauxPropres = BigDecimal.valueOf(100000000);
        }

        BigDecimal totalActif = calculerSoldeDebitMoinsCredit(agregats, "2").abs()
                .add(calculerSoldeDebitMoinsCredit(agregats, "3").abs())
                .add(calculerSoldeDebitMoinsCredit(agregats, "5").abs());
        if (totalActif.compareTo(BigDecimal.ZERO) == 0) {
            totalActif = BigDecimal.valueOf(250000000);
        }

        BigDecimal roe = resultatNet.multiply(BigDecimal.valueOf(100)).divide(capitauxPropres, 2, RoundingMode.HALF_UP);
        BigDecimal roa = resultatNet.multiply(BigDecimal.valueOf(100)).divide(totalActif, 2, RoundingMode.HALF_UP);

        BigDecimal actCourtTerme = calculerSoldeDebitMoinsCredit(agregats, "5").abs()
                .add(calculerSoldeDebitMoinsCredit(agregats, "3").abs());
        BigDecimal detCourtTerme = calculerSoldeDebitMoinsCredit(agregats, "4").abs();
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
        String tiersNom = tiersRepository.findById(saved.getTiersId())
                .map(TiersEntity::getRaisonSociale)
                .orElse("Tiers inconnu");
        return mapEngagement(saved, tiersNom);
    }

    public List<EngagementHorsBilanOutput> genererReportingHorsBilan() {
        List<EngagementHorsBilanEntity> engagements = engagementHorsBilanRepository.findAll();
        Set<UUID> tiersIds = engagements.stream()
                .map(EngagementHorsBilanEntity::getTiersId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> tiersNames = tiersIds.isEmpty()
                ? Map.of()
                : tiersRepository.findAllById(tiersIds).stream()
                        .collect(Collectors.toMap(TiersEntity::getId, TiersEntity::getRaisonSociale, (a, b) -> a));

        return engagements.stream()
                .map(e -> mapEngagement(e, tiersNames.getOrDefault(e.getTiersId(), "Tiers inconnu")))
                .collect(Collectors.toList());
    }

    private EngagementHorsBilanOutput mapEngagement(EngagementHorsBilanEntity e, String tiersNom) {
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

    private BigDecimal calculerSoldeDebitMoinsCredit(List<Object[]> agregats, String prefix) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Object[] row : agregats) {
            String compteCode = (String) row[0];
            if (compteCode != null && compteCode.startsWith(prefix)) {
                if (row[1] != null) totalDebit = totalDebit.add((BigDecimal) row[1]);
                if (row[2] != null) totalCredit = totalCredit.add((BigDecimal) row[2]);
            }
        }
        return totalDebit.subtract(totalCredit);
    }

    private BigDecimal calculerSoldeCreditMoinsDebit(List<Object[]> agregats, String prefix) {
        return calculerSoldeDebitMoinsCredit(agregats, prefix).negate();
    }
}
