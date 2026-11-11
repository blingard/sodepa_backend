package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CouvertureInput;
import com.sodepa.erp.budget.application.inputs.RechercheCouvertureInput;
import com.sodepa.erp.budget.application.outputs.ContratCouvertureOutput;
import com.sodepa.erp.budget.application.outputs.ValuationCouvertureReportOutput;
import com.sodepa.erp.budget.application.usecase.EnregistrerCouvertureUseCase;
import com.sodepa.erp.budget.application.usecase.EvaluerEcartsChangeLatentsUseCase;
import com.sodepa.erp.budget.application.usecase.ListerCouverturesUseCase;
import com.sodepa.erp.budget.presentation.requests.CouvertureRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service de Gestion du Risque de Change et des Instruments de Couverture a Terme (Module 02 - BUD / Trésorerie).
 * <p>
 * Ce service protege les marges financieres de la SODEPA lors de ses operations commerciales en devises etrangeres :
 * <ul>
 *   <li>Importation d'intrants veterinaire, de materiels pastoraux ou de pieces d'abattoir factures en devises (EUR, USD).</li>
 *   <li>Souscription et enregistrement des contrats de couverture a terme (Forward, Hedging) conclus aupres des banques partenaires.</li>
 *   <li>Reevaluation comptable periodique au cours du jour (Mark-to-Market) pour quantifier les gains ou pertes de change latents.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tresorerie/change")
@RequiredArgsConstructor
@Tag(name = "Gestion du Risque de Change (Hedging)", description = "Portefeuille de contrats de change à terme et valorisation Mark-to-Market")
public class ChangeHedgingRestController {

    private final EnregistrerCouvertureUseCase enregistrerCouvertureUseCase;
    private final EvaluerEcartsChangeLatentsUseCase evaluerEcartsChangeLatentsUseCase;
    private final ListerCouverturesUseCase listerCouverturesUseCase;

    /**
     * Consulte le portefeuille complet des contrats de couverture de change souscrits.
     * <p>
     * Permet aux tresoriers de suivre les positions ouvertes, les montants garantis et les echeances.
     *
     * @param devise filtre optionnel par devise couverte (ex: 'USD', 'EUR')
     * @param statut filtre optionnel sur le statut du contrat (ACTIF, ECHU, LIQUIDE)
     * @return la liste des instruments de couverture correspondants
     */
    @GetMapping("/couverture")
    @Operation(summary = "Lister les contrats de couverture de change à terme")
    public List<ContratCouvertureOutput> listerCouvertures(
            @RequestParam(required = false) String devise,
            @RequestParam(required = false) String statut
    ) {
        return listerCouverturesUseCase.execute(new RechercheCouvertureInput(devise, statut));
    }

    /**
     * Enregistre un nouveau contrat bancaire de couverture de change a terme.
     * <p>
     * Fige un cours de change garanti contractuellement avec la banque afin de neutraliser
     * le risque de depreciation du Franc CFA sur un achat a l'international.
     *
     * @param request caracteristiques de la couverture (reference bancaire, devise, montant, cours fige, dates d'effet et d'echeance)
     * @return le contrat de couverture formalise
     */
    @PostMapping("/couverture")
    @Operation(summary = "Enregistrer un nouveau contrat de couverture de change")
    public ContratCouvertureOutput enregistrerCouverture(@Valid @RequestBody CouvertureRequest request) {
        return enregistrerCouvertureUseCase.execute(
                new CouvertureInput(request.reference(), request.devise(), request.montantDevise(), request.coursGaranti(), request.dateEffet(), request.dateEcheance())
        );
    }

    /**
     * Evalue la valeur liquidative instantanee (Mark-to-Market) d'un contrat de couverture.
     * <p>
     * Compare le cours garanti negocie avec le cours spot actuel du marche des devises
     * afin de determiner le gain ou la perte de change latente a comptabiliser conformement aux regles OHADA.
     *
     * @param id identifiant unique du contrat a valoriser
     * @param coursSpot cours actuel de la devise sur le marche interbancaire
     * @return le rapport d'evaluation de valorisation et l'ecart latent calcule
     */
    @GetMapping("/couverture/{id}/evaluer")
    @Operation(summary = "Valoriser un contrat de couverture au cours du jour (Mark-to-Market)")
    public ValuationCouvertureReportOutput evaluer(@PathVariable UUID id, @RequestParam BigDecimal coursSpot) {
        return evaluerEcartsChangeLatentsUseCase.execute(new EvaluerEcartsChangeLatentsUseCase.Input(id, coursSpot));
    }
}
