package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.PreEngagementInput;
import com.sodepa.erp.budget.application.inputs.RejeterInput;
import com.sodepa.erp.budget.application.inputs.ValiderEtapeInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.application.usecase.RejeterEngagementUseCase;
import com.sodepa.erp.budget.application.usecase.SoumettrePreEngagementUseCase;
import com.sodepa.erp.budget.application.usecase.ValiderEtapeWorkflowUseCase;
import com.sodepa.erp.budget.presentation.requests.PreEngagementRequest;
import com.sodepa.erp.budget.presentation.requests.RejeterRequest;
import com.sodepa.erp.budget.presentation.requests.ValiderEtapeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST gérant le circuit d'approbation et de validation multiniveaux des engagements budgétaires.
 */
@RestController
@RequestMapping("/api/budget/engagements/workflow")
@RequiredArgsConstructor
public class EngagementWorkflowRestController {

    private final SoumettrePreEngagementUseCase soumettrePreEngagementUseCase;
    private final ValiderEtapeWorkflowUseCase validerEtapeWorkflowUseCase;
    private final RejeterEngagementUseCase rejeterEngagementUseCase;

    /**
     * Soumet un nouveau pré-engagement de dépense (bon de commande temporaire).
     */
    @PostMapping("/pre-engager")
    public BudgetEngagementOutput preEngager(@Valid @RequestBody PreEngagementRequest request) {
        return soumettrePreEngagementUseCase.execute(
                new PreEngagementInput(request.planId(), request.compteCode(), request.sectionId(), request.numeroEngagement(), request.description(), request.montant(), request.utilisateurId())
        );
    }

    /**
     * Applique la validation d'une étape par un approbateur habilité.
     */
    @PostMapping("/valider")
    public void validerEtape(@Valid @RequestBody ValiderEtapeRequest request) {
        validerEtapeWorkflowUseCase.execute(
                new ValiderEtapeInput(request.numeroEngagement(), request.roleApprobateur(), request.utilisateurId())
        );
    }

    /**
     * Rejette définitivement un engagement budgétaire et libère l'enveloppe réservée.
     */
    @PostMapping("/rejeter")
    public void rejeter(@Valid @RequestBody RejeterRequest request) {
        rejeterEngagementUseCase.execute(
                new RejeterInput(request.numeroEngagement(), request.motif(), request.utilisateurId())
        );
    }
}
