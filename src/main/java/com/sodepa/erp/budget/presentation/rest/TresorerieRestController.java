package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CreerPrevisionInput;
import com.sodepa.erp.budget.application.outputs.BfrReportOutput;
import com.sodepa.erp.budget.application.outputs.CashFlowMensuelOutput;
import com.sodepa.erp.budget.application.outputs.OverdraftAlertOutput;
import com.sodepa.erp.budget.application.outputs.PrevisionTresorerieOutput;
import com.sodepa.erp.budget.application.outputs.SimulationResultOutput;
import com.sodepa.erp.budget.application.usecase.*;
import com.sodepa.erp.budget.presentation.requests.CreerPrevisionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur REST pour le suivi de trésorerie, cash-flow prévisionnel, alertes découverts, BFR
 * et exécution des simulations financières What-If.
 */
@RestController
@RequestMapping("/api/tresorerie")
@RequiredArgsConstructor
public class TresorerieRestController {

    private final AjouterPrevisionUseCase ajouterPrevisionUseCase;
    private final GenererCashFlowPrevisionnelUseCase genererCashFlowPrevisionnelUseCase;
    private final CalculerBfrUseCase calculerBfrUseCase;
    private final VerifierSeuilsDecouvertUseCase verifierSeuilsDecouvertUseCase;
    private final SimulerHypothesesWhatIfUseCase simulerHypothesesWhatIfUseCase;

    /**
     * Ajoute une prévision d'encaissement ou de décaissement.
     */
    @PostMapping("/previsions")
    public PrevisionTresorerieOutput ajouterPrevision(@Valid @RequestBody CreerPrevisionRequest request) {
        return ajouterPrevisionUseCase.execute(
                new CreerPrevisionInput(request.dateEcheance(), request.type(), request.source(), request.libelle(), request.montant())
        );
    }

    /**
     * Génère le plan de cash-flow prévisionnel mensuel.
     * 
     * @param debut date de début
     * @param fin date de fin
     * @return le plan mensuel
     */
    @GetMapping("/cash-flow")
    public List<CashFlowMensuelOutput> genererCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return genererCashFlowPrevisionnelUseCase.execute(new GenererCashFlowPrevisionnelUseCase.Input(debut, fin));
    }

    /**
     * Calcule le BFR prévisionnel à 30 jours à une date donnée.
     */
    @GetMapping("/bfr")
    public BfrReportOutput calculerBfr(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return calculerBfrUseCase.execute(date);
    }

    /**
     * Déclenche la vérification des lignes de découvert et renvoie les alertes de liquidité actives.
     */
    @GetMapping("/decouverts/alertes")
    public List<OverdraftAlertOutput> verifierAlertesDecouvert() {
        return verifierSeuilsDecouvertUseCase.execute(null);
    }

    /**
     * Exécute une simulation financière What-If à partir d'hypothèses de croissance, d'inflation et de prix de revient.
     */
    @GetMapping("/simulations/what-if")
    public SimulationResultOutput simuler(
            @RequestParam BigDecimal croissance,
            @RequestParam BigDecimal inflation,
            @RequestParam BigDecimal prixRevient) {
        return simulerHypothesesWhatIfUseCase.execute(new SimulerHypothesesWhatIfUseCase.Input(croissance, inflation, prixRevient));
    }
}
