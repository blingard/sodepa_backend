package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.outputs.RecommandationPaiementOutput;
import com.sodepa.erp.budget.application.usecase.MatchingAutomatiqueUseCase;
import com.sodepa.erp.budget.application.usecase.RecommanderArbitrageDecaissementsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * Service de Rapprochement Bancaire et d'Arbitrage des Paiements Fournisseurs (Module 02 - BUD / Trésorerie).
 * <p>
 * Ce service offre deux fonctionnalites cles pour la gestion quotidienne de la tresorerie :
 * <ul>
 *   <li><b>Rapprochement bancaire automatise</b> : Appariement automatique des lignes du releve bancaire avec les ecritures comptables d'engagement et de reglement.</li>
 *   <li><b>Arbitrage des decaissements</b> : Algorithme d'aide a la decision qui planifie les paiements fournisseurs a honorer en priorite tout en preservant un matelas de securite financier minimal.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tresorerie/rapprochement")
@RequiredArgsConstructor
@Tag(name = "Rapprochement Bancaire & Décaissements", description = "Pointage bancaire automatisé et arbitrage de trésorerie sur les paiements fournisseurs")
public class RapprochementBancaireRestController {

    private final MatchingAutomatiqueUseCase matchingAutomatiqueUseCase;
    private final RecommanderArbitrageDecaissementsUseCase recommanderArbitrageDecaissementsUseCase;

    /**
     * Declenche le pointage automatique entre un releve bancaire importe et la comptabilite de la SODEPA.
     * <p>
     * Rapproche les flux par concordance de montant, de date de valeur et de reference de virement/cheque.
     * 
     * @param releveId identifiant du releve bancaire importe
     * @return synthese du traitement contenant le nombre de lignes automatiquement lettrées
     */
    @PostMapping("/matching")
    @Operation(summary = "Lancer le rapprochement bancaire automatique")
    public Map<String, Object> matcherReleve(@RequestParam UUID releveId) {
        Integer count = matchingAutomatiqueUseCase.execute(releveId);
        return Map.of("message", count + " lignes de relevé bancaire ont été rapprochées automatiquement avec succès.", "count", count);
    }

    /**
     * Calcule et recommande un plan de decaissement optimal pour le reglement des factures fournisseurs.
     * <p>
     * Classe les factures en attente par date d'echeance, importance strategique et escompte eventuel,
     * en veillant a ce que le solde bancaire de la SODEPA ne descende jamais sous le fonds de reserve minimal.
     *
     * @param fondsSecurite montant plancher de tresorerie a conserver obligatoirement (fonds de roulement de securite)
     * @param debut debut de la fenetre temporelle d'arbitrage
     * @param fin fin de la fenetre d'arbitrage
     * @param soldeActuel disponibilite bancaire immediate constatee
     * @return la liste des paiements conseilles et planifies par ordre de priorite
     */
    @GetMapping("/arbitrage")
    @Operation(summary = "Calculer l'arbitrage optimal des paiements fournisseurs")
    public List<RecommandationPaiementOutput> genererArbitrage(
            @RequestParam BigDecimal fondsSecurite,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam BigDecimal soldeActuel) {
        return recommanderArbitrageDecaissementsUseCase.execute(new RecommanderArbitrageDecaissementsUseCase.Input(fondsSecurite, debut, fin, soldeActuel));
    }
}
