package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CreerFinancementInput;
import com.sodepa.erp.budget.application.inputs.CreerHorsBilanInput;
import com.sodepa.erp.budget.application.outputs.EcheanceOutput;
import com.sodepa.erp.budget.application.outputs.EngagementHorsBilanOutput;
import com.sodepa.erp.budget.application.outputs.FinancementOutput;
import com.sodepa.erp.budget.application.outputs.KpiReportOutput;
import com.sodepa.erp.budget.application.usecase.CalculerKpisPerformancesUseCase;
import com.sodepa.erp.budget.application.usecase.EnregistrerEngagementHorsBilanUseCase;
import com.sodepa.erp.budget.application.usecase.EnregistrerFinancementUseCase;
import com.sodepa.erp.budget.application.usecase.EnregistrerReglementEcheanceUseCase;
import com.sodepa.erp.budget.application.usecase.GenererReportingHorsBilanUseCase;
import com.sodepa.erp.budget.application.usecase.SimulerAmortissementUseCase;
import com.sodepa.erp.budget.presentation.requests.CreerFinancementRequest;
import com.sodepa.erp.budget.presentation.requests.CreerHorsBilanRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des emprunts, amortissements de prêts,
 * engagements hors-bilan (OHADA 34-1) et indicateurs financiers de performance.
 */
@RestController
@RequestMapping("/api/financement")
@RequiredArgsConstructor
public class FinancementRestController {

    private final EnregistrerFinancementUseCase enregistrerFinancementUseCase;
    private final SimulerAmortissementUseCase simulerAmortissementUseCase;
    private final EnregistrerReglementEcheanceUseCase enregistrerReglementEcheanceUseCase;
    private final EnregistrerEngagementHorsBilanUseCase enregistrerEngagementHorsBilanUseCase;
    private final GenererReportingHorsBilanUseCase genererReportingHorsBilanUseCase;
    private final CalculerKpisPerformancesUseCase calculerKpisPerformancesUseCase;

    /**
     * Enregistre un nouveau financement (avec son plan d'amortissement et l'écriture comptable d'entrée de fonds).
     */
    @PostMapping
    public FinancementOutput enregistrerFinancement(@Valid @RequestBody CreerFinancementRequest request) {
        CreerFinancementInput input = new CreerFinancementInput(
                request.banqueId(), request.intitule(), request.type(),
                request.capital(), request.tauxNominal(),
                request.dateEffet(), request.dureeMois(),
                request.periodicite(), request.utilisateurId()
        );
        return enregistrerFinancementUseCase.execute(input);
    }

    /**
     * Simule un plan d'amortissement sans l'enregistrer.
     */
    @GetMapping("/simuler")
    public List<EcheanceOutput> simulerAmortissement(
            @RequestParam BigDecimal capital,
            @RequestParam BigDecimal tauxNominal,
            @RequestParam int dureeMois,
            @RequestParam String periodicite,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEffet) {
        CreerFinancementInput input = new CreerFinancementInput(null, null, null, capital, tauxNominal, dateEffet, dureeMois, periodicite, null);
        return simulerAmortissementUseCase.execute(input);
    }

    /**
     * Enregistre et valide le paiement d'une échéance (avec passation d'écritures automatiques).
     */
    @PostMapping("/echeances/{echeanceId}/payer")
    public void payerEcheance(@PathVariable UUID echeanceId, @RequestParam UUID userId) {
        enregistrerReglementEcheanceUseCase.execute(new EnregistrerReglementEcheanceUseCase.Input(echeanceId, userId));
    }

    /**
     * Enregistre un nouvel engagement hors-bilan (cautionnement, garantie, etc.).
     */
    @PostMapping("/hors-bilan")
    public EngagementHorsBilanOutput enregistrerHorsBilan(@Valid @RequestBody CreerHorsBilanRequest request) {
        CreerHorsBilanInput input = new CreerHorsBilanInput(
                request.type(), request.intitule(), request.tiersId(),
                request.montant(), request.dateEffet(), request.dateEcheance()
        );
        return enregistrerEngagementHorsBilanUseCase.execute(input);
    }

    /**
     * Extrait l'état de reporting des engagements hors-bilan requis par la norme OHADA (article 34-1).
     */
    @GetMapping("/reporting/hors-bilan")
    public List<EngagementHorsBilanOutput> genererReportingHorsBilan() {
        return genererReportingHorsBilanUseCase.execute(null);
    }

    /**
     * Calcule et renvoie les ratios financiers consolidés (ROE, ROA, Liquidité).
     */
    @GetMapping("/reporting/kpis")
    public KpiReportOutput genererReportingKpis() {
        return calculerKpisPerformancesUseCase.execute(null);
    }
}
