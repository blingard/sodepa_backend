package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ReevaluationInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReevaluationLineOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.ClotureExerciceUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.EcartConversionUseCase;
import com.sodepa.erp.comptabilite.generale.presentation.requests.ReevaluationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST exposant les points de terminaison pour `ClotureRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping("/api/comptabilite/cloture")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DAF', 'DG') or hasAnyAuthority('INIT_CREATE_FISCAL_YEAR_CLOSING', 'BUDGET_CLOTURE_EXERCICE', 'VALIDATE_OR_REJECT_ECRITURE')")
public class ClotureRestController {

    private final ClotureExerciceUseCase clotureExerciceUseCase;
    private final EcartConversionUseCase ecartConversionUseCase;

    @PostMapping("/{annee}")
    public void cloturerExercice(@PathVariable int annee) {
        clotureExerciceUseCase.execute(annee);
    }

    @PostMapping("/reevaluer")
    public List<ReevaluationLineOutput> reevaluerDevises(@RequestBody @Valid ReevaluationRequest request) {
        ReevaluationInput input = new ReevaluationInput(request.annee(), request.coursCloture());
        return ecartConversionUseCase.execute(input);
    }
}
