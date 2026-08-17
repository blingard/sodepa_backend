package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.outputs.RunwayReportOutput;
import com.sodepa.erp.budget.application.outputs.TftOhadaReportOutput;
import com.sodepa.erp.budget.application.usecase.CalculerRunwayAndBurnRateUseCase;
import com.sodepa.erp.budget.application.usecase.GenererTftOhadaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'analyse stratégique (TFT OHADA, Runway, Burn Rate).
 */
@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
public class PilotageStrategiqueRestController {

    private final GenererTftOhadaUseCase genererTftOhadaUseCase;
    private final CalculerRunwayAndBurnRateUseCase calculerRunwayAndBurnRateUseCase;

    /**
     * Génère le Tableau des Flux de Trésorerie (TFT) réglementaire OHADA pour un exercice donné.
     */
    @GetMapping("/tft")
    public TftOhadaReportOutput genererTft(@RequestParam int annee) {
        return genererTftOhadaUseCase.execute(annee);
    }

    /**
     * Calcule l'autonomie financière de l'entreprise (Runway en mois et Cash Burn Rate).
     */
    @GetMapping("/runway")
    public RunwayReportOutput calculerRunway() {
        return calculerRunwayAndBurnRateUseCase.execute(null);
    }
}
