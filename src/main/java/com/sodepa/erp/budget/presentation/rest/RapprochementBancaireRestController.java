package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.outputs.RecommandationPaiementOutput;
import com.sodepa.erp.budget.application.usecase.MatchingAutomatiqueUseCase;
import com.sodepa.erp.budget.application.usecase.RecommanderArbitrageDecaissementsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * Contrôleur REST pour le rapprochement bancaire et l'arbitrage des décaissements fournisseurs.
 */
@RestController
@RequestMapping("/api/tresorerie/rapprochement")
@RequiredArgsConstructor
public class RapprochementBancaireRestController {

    private final MatchingAutomatiqueUseCase matchingAutomatiqueUseCase;
    private final RecommanderArbitrageDecaissementsUseCase recommanderArbitrageDecaissementsUseCase;

    /**
     * Déclenche le matching automatique (appariement intelligent) d'un relevé bancaire importé.
     * 
     * @param releveId l'identifiant du relevé
     * @return le nombre de lignes réelles lettrées / appariées
     */
    @PostMapping("/matching")
    public Map<String, Object> matcherReleve(@RequestParam UUID releveId) {
        Integer count = matchingAutomatiqueUseCase.execute(releveId);
        return Map.of("message", count + " lignes de relevé bancaire ont été rapprochées automatiquement avec succès.", "count", count);
    }

    /**
     * Obtient le rapport d'arbitrage de planification des règlements fournisseurs.
     */
    @GetMapping("/arbitrage")
    public List<RecommandationPaiementOutput> genererArbitrage(
            @RequestParam BigDecimal fondsSecurite,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam BigDecimal soldeActuel) {
        return recommanderArbitrageDecaissementsUseCase.execute(new RecommanderArbitrageDecaissementsUseCase.Input(fondsSecurite, debut, fin, soldeActuel));
    }
}
