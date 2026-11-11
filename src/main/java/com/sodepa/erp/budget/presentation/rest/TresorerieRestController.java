package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CreerPrevisionInput;
import com.sodepa.erp.budget.application.inputs.PeriodeInput;
import com.sodepa.erp.budget.application.outputs.BfrReportOutput;
import com.sodepa.erp.budget.application.outputs.CashFlowMensuelOutput;
import com.sodepa.erp.budget.application.outputs.OverdraftAlertOutput;
import com.sodepa.erp.budget.application.outputs.PrevisionTresorerieOutput;
import com.sodepa.erp.budget.application.outputs.SimulationResultOutput;
import com.sodepa.erp.budget.application.usecase.*;
import com.sodepa.erp.budget.presentation.requests.CreerPrevisionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service de Gestion Previsionnelle de la Tresorerie et de Modelisation Financiere (Module 02 - BUD / Trésorerie).
 * <p>
 * Ce service offre une visibilite prospective sur les liquidites de la SODEPA :
 * <ul>
 *   <li>Enregistrement et suivi des echeances previsionnelles d'encaissement et de decaissement.</li>
 *   <li>Plan glissant de cash-flow mensuel agregeant les recettes de vente et les charges d'exploitation.</li>
 *   <li>Calcul et suivi du Besoin en Fonds de Roulement (BFR) d'exploitation.</li>
 *   <li>Detection precoce et alertes automatiques sur le depassement des lignes de decouvert bancaire autorisees.</li>
 *   <li>Simulations prospectives What-If testant la resilience financiere selon des scenarios macro-economiques (inflation, hausse des intrants, croissance des ventes).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tresorerie")
@RequiredArgsConstructor
@Tag(name = "Gestion Prévisionnelle de Trésorerie", description = "Cash-flow mensuel, calcul du BFR, alertes découverts et simulations What-If")
public class TresorerieRestController {

    private final AjouterPrevisionUseCase ajouterPrevisionUseCase;
    private final GenererCashFlowPrevisionnelUseCase genererCashFlowPrevisionnelUseCase;
    private final CalculerBfrUseCase calculerBfrUseCase;
    private final VerifierSeuilsDecouvertUseCase verifierSeuilsDecouvertUseCase;
    private final SimulerHypothesesWhatIfUseCase simulerHypothesesWhatIfUseCase;
    private final ListerPrevisionsUseCase listerPrevisionsUseCase;

    /**
     * Liste les flux de tresorerie previsionnels attendus sur un intervalle temporel defini.
     * <p>
     * Restitue le detail echeance par echeance utilise pour construire le plan de tresorerie glissant.
     *
     * @param debut date de debut de la periode
     * @param fin date de fin de la periode
     * @return la liste des encaissements et decaissements attendus
     */
    @GetMapping("/previsions")
    @Operation(summary = "Lister les échéances prévisionnelles de trésorerie sur une période")
    public List<PrevisionTresorerieOutput> listerPrevisions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return listerPrevisionsUseCase.execute(new PeriodeInput(debut, fin));
    }

    /**
     * Enregistre une nouvelle pre-vision d'encaissement ou de decaissement de fonds.
     *
     * @param request informations du flux attendu (date d'echeance, type recette/depense, nature, libelle et montant)
     * @return la prevision de tresorerie creee
     */
    @PostMapping("/previsions")
    @Operation(summary = "Ajouter une prévision d'encaissement ou de décaissement")
    public PrevisionTresorerieOutput ajouterPrevision(@Valid @RequestBody CreerPrevisionRequest request) {
        return ajouterPrevisionUseCase.execute(
                new CreerPrevisionInput(request.dateEcheance(), request.type(), request.source(), request.libelle(), request.montant())
        );
    }

    /**
     * Construit le plan de flux de tresorerie (Cash-Flow) mensuel previsionnel.
     * <p>
     * Agrege l'ensemble des encaissements et des decaissements mois par mois afin d'identifier
     * les mois excedentaires et les periodes potentielles de deficit de liquidite.
     * 
     * @param debut date de debut de l'analyse
     * @param fin date de fin de l'analyse
     * @return le tableau mensuel des soldes de cash-flow
     */
    @GetMapping("/cash-flow")
    @Operation(summary = "Générer le plan de cash-flow prévisionnel mensuel")
    public List<CashFlowMensuelOutput> genererCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return genererCashFlowPrevisionnelUseCase.execute(new GenererCashFlowPrevisionnelUseCase.Input(debut, fin));
    }

    /**
     * Calcule le Besoin en Fonds de Roulement (BFR) operationnel a un horizon de 30 jours.
     * <p>
     * Evalue le montant de liquidites necessaire pour financer le decalage entre les encaissements clients
     * (bouchers, distributeurs) et les decaissements (fournisseurs d'aliments, soins veterinaires, salaires).
     *
     * @param date date d'arrete pour le calcul du BFR
     * @return le rapport d'analyse du BFR
     */
    @GetMapping("/bfr")
    @Operation(summary = "Calculer le Besoin en Fonds de Roulement (BFR) prévisionnel")
    public BfrReportOutput calculerBfr(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return calculerBfrUseCase.execute(date);
    }

    /**
     * Controle les positions de tresorerie courantes et declenche les alertes en cas de risque de decouvert.
     * <p>
     * Compare les soldes bancaires aux limites de facilites de caisse accordees par les partenaires financiers.
     *
     * @return la liste des alertes de tension ou de depassement de decouvert en cours
     */
    @GetMapping("/decouverts/alertes")
    @Operation(summary = "Vérifier les seuils de découverts et récupérer les alertes de liquidité")
    public List<OverdraftAlertOutput> verifierAlertesDecouvert() {
        return verifierSeuilsDecouvertUseCase.execute(null);
    }

    /**
     * Execute une simulation de stress-test et de sensibilite financiere (What-If).
     * <p>
     * Permet aux decideurs de tester l'impact d'une conjoncture defavorable ou favorable sur la rentabilite :
     * variation des cours de la viande bovine, choc d'inflation ou augmentation du cout de revient des intrants.
     *
     * @param croissance taux de croissance ou regression du volume d'activite (en %)
     * @param inflation taux d'inflation estime affectant les charges (en %)
     * @param prixRevient variation estimee du prix de revient de production animale (en %)
     * @return la projection de resultat d'exploitation simulee
     */
    @GetMapping("/simulations/what-if")
    @Operation(summary = "Exécuter une simulation financière prospective What-If")
    public SimulationResultOutput simuler(
            @RequestParam BigDecimal croissance,
            @RequestParam BigDecimal inflation,
            @RequestParam BigDecimal prixRevient) {
        return simulerHypothesesWhatIfUseCase.execute(new SimulerHypothesesWhatIfUseCase.Input(croissance, inflation, prixRevient));
    }
}
