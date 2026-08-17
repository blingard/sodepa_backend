package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CouvertureInput;
import com.sodepa.erp.budget.application.outputs.ContratCouvertureOutput;
import com.sodepa.erp.budget.application.outputs.ValuationCouvertureReportOutput;
import com.sodepa.erp.budget.application.usecase.EnregistrerCouvertureUseCase;
import com.sodepa.erp.budget.application.usecase.EvaluerEcartsChangeLatentsUseCase;
import com.sodepa.erp.budget.presentation.requests.CouvertureRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contrôleur REST pour le suivi des instruments de couverture de change à terme (Hedging).
 */
@RestController
@RequestMapping("/api/tresorerie/change")
@RequiredArgsConstructor
public class ChangeHedgingRestController {

    private final EnregistrerCouvertureUseCase enregistrerCouvertureUseCase;
    private final EvaluerEcartsChangeLatentsUseCase evaluerEcartsChangeLatentsUseCase;

    /**
     * Enregistre un nouveau contrat de couverture de change.
     */
    @PostMapping("/couverture")
    public ContratCouvertureOutput enregistrerCouverture(@Valid @RequestBody CouvertureRequest request) {
        return enregistrerCouvertureUseCase.execute(
                new CouvertureInput(request.reference(), request.devise(), request.montantDevise(), request.coursGaranti(), request.dateEffet(), request.dateEcheance())
        );
    }

    /**
     * Évalue le Mark-to-Market d'un contrat de couverture à partir d'un cours spot actuel de marché.
     */
    @GetMapping("/couverture/{id}/evaluer")
    public ValuationCouvertureReportOutput evaluer(@PathVariable UUID id, @RequestParam BigDecimal coursSpot) {
        return evaluerEcartsChangeLatentsUseCase.execute(new EvaluerEcartsChangeLatentsUseCase.Input(id, coursSpot));
    }
}
