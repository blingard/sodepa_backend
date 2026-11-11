package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.outputs.RunwayReportOutput;
import com.sodepa.erp.budget.application.outputs.TftOhadaReportOutput;
import com.sodepa.erp.budget.application.usecase.CalculerRunwayAndBurnRateUseCase;
import com.sodepa.erp.budget.application.usecase.GenererTftOhadaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Service d'aide a la decision et de pilotage financier strategique (Module 02 - BUD).
 * <p>
 * Ce point d'acces fournit aux organes de direction (Directeur General, DAF, Conseil d'Administration)
 * des indicateurs de soutenabilite financiere et des etats reglementaires standardises :
 * <ul>
 *   <li>Le Tableau des Flux de Tresorerie (TFT) conforme aux normes comptables OHADA (activites d'exploitation, investissements, financement).</li>
 *   <li>L'analyse de la vitesse de consommation de liquidites (Burn Rate mensuel) pour anticiper les tensions de tresorerie.</li>
 *   <li>L'autonomie financiere nette (Runway exprime en mois d'exploitation restants).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
@Tag(name = "Pilotage Stratégique & Indicateurs", description = "Tableau des flux de trésorerie OHADA, autonomie financière et vitesse de dépense")
public class PilotageStrategiqueRestController {

    private final GenererTftOhadaUseCase genererTftOhadaUseCase;
    private final CalculerRunwayAndBurnRateUseCase calculerRunwayAndBurnRateUseCase;

    /**
     * Etablit et restitue le Tableau des Flux de Tresorerie (TFT) reglementaire OHADA pour un exercice.
     * <p>
     * Synthetise les encaissements et decaissements effectifs selon les trois rubriques fondamentales :
     * flux de tresorerie lies aux operations d'exploitation (ventes de betail, viandes, intrants),
     * operations d'investissement (infrastructures pastorales, abattoirs) et operations de financement (subventions, emprunts).
     *
     * @param annee exercice budgetaire et comptable a analyser (ex: 2026)
     * @return l'etat financier complet structure selon la maquette OHADA
     */
    @GetMapping("/tft")
    @Operation(summary = "Générer le Tableau des Flux de Trésorerie (TFT) OHADA")
    public TftOhadaReportOutput genererTft(@RequestParam int annee) {
        return genererTftOhadaUseCase.execute(annee);
    }

    /**
     * Evalue la visibilite de tresorerie de la SODEPA (indicateurs de Runway et de Burn Rate).
     * <p>
     * Calcule la consommation moyenne mensuelle de tresorerie et le nombre de mois pendant lesquels
     * la societe peut assurer la continuite de ses charges operationnelles courantes
     * sans nouvel apport de liquidites.
     *
     * @return les indicateurs d'autonomie financiere et le rythme moyen de decaissement
     */
    @GetMapping("/runway")
    @Operation(summary = "Calculer l'autonomie financière (Runway en mois et rythme de consommation de trésorerie)")
    public RunwayReportOutput calculerRunway() {
        return calculerRunwayAndBurnRateUseCase.execute(null);
    }
}
