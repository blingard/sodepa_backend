package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.AjouterItemInput;
import com.sodepa.erp.budget.application.inputs.CreerBudgetPlanInput;
import com.sodepa.erp.budget.application.inputs.EngagementInput;
import com.sodepa.erp.budget.application.inputs.ReallocationInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.application.outputs.BudgetItemOutput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanOutput;
import com.sodepa.erp.budget.application.usecase.*;
import com.sodepa.erp.budget.presentation.requests.AjouterItemRequest;
import com.sodepa.erp.budget.presentation.requests.CreerBudgetRequest;
import com.sodepa.erp.budget.presentation.requests.EngagementRequest;
import com.sodepa.erp.budget.presentation.requests.ReallocationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur REST pour l'élaboration de budgets, le workflow de validation,
 * les réallocations d'enveloppes et le suivi des engagements.
 */
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetPlanRestController {

    private final CreerBudgetPlanUseCase creerBudgetPlanUseCase;
    private final AjouterItemPlanUseCase ajouterItemPlanUseCase;
    private final SoumettrePlanUseCase soumettrePlanUseCase;
    private final ApprouverPlanUseCase approuverPlanUseCase;
    private final RejeterPlanUseCase rejeterPlanUseCase;
    private final EffectuerReallocationUseCase effectuerReallocationUseCase;
    private final EnregistrerEngagementUseCase enregistrerEngagementUseCase;
    private final LiquiderEngagementUseCase liquiderEngagementUseCase;
    private final AnnulerEngagementUseCase annulerEngagementUseCase;

    /**
     * Crée un nouveau plan budgétaire annuel.
     * 
     * @param request la requête contenant l'année et le libellé
     * @return le budget créé
     */
    @PostMapping("/plans")
    public BudgetPlanOutput creerBudgetPlan(@Valid @RequestBody CreerBudgetRequest request) {
        return creerBudgetPlanUseCase.execute(
                new CreerBudgetPlanInput(request.annee(), request.intitule(), request.utilisateurId())
        );
    }

    /**
     * Ajoute une ligne budgétaire détaillée à un plan.
     * 
     * @param planId l'identifiant du plan
     * @param request la ligne à ajouter (compte, section, montant)
     * @return l'item budgétaire créé
     */
    @PostMapping("/plans/{planId}/items")
    public BudgetItemOutput ajouterItem(@PathVariable UUID planId, @Valid @RequestBody AjouterItemRequest request) {
        return ajouterItemPlanUseCase.execute(
                new AjouterItemInput(planId, request.compteCode(), request.sectionId(), request.montant())
        );
    }

    /**
     * Soumet un plan budgétaire pour validation.
     */
    @PostMapping("/plans/{planId}/soumettre")
    public void soumettrePlan(@PathVariable UUID planId, @RequestParam UUID userId) {
        soumettrePlanUseCase.execute(new SoumettrePlanUseCase.Input(planId, userId));
    }

    /**
     * Approuve et publie définitivement un plan budgétaire.
     */
    @PostMapping("/plans/{planId}/approuver")
    public void approuverPlan(@PathVariable UUID planId, @RequestParam UUID userId) {
        approuverPlanUseCase.execute(new ApprouverPlanUseCase.Input(planId, userId));
    }

    /**
     * Rejette un plan budgétaire.
     */
    @PostMapping("/plans/{planId}/rejeter")
    public void rejeterPlan(@PathVariable UUID planId, @RequestParam UUID userId) {
        rejeterPlanUseCase.execute(new RejeterPlanUseCase.Input(planId, userId));
    }

    /**
     * Transfère une enveloppe budgétaire d'une ligne à une autre.
     */
    @PostMapping("/reallocations")
    public void reallocer(@Valid @RequestBody ReallocationRequest request) {
        effectuerReallocationUseCase.execute(
                new ReallocationInput(request.sourceItemId(), request.destItemId(), request.montant(), request.responsableId(), request.raison())
        );
    }

    /**
     * Enregistre un nouvel engagement de dépense (bon de commande).
     * Effectue le contrôle de dépassement budgétaire bloquant.
     */
    @PostMapping("/engagements")
    public BudgetEngagementOutput engager(@Valid @RequestBody EngagementRequest request) {
        return enregistrerEngagementUseCase.execute(
                new EngagementInput(request.planId(), request.compteCode(), request.sectionId(), request.numeroEngagement(), request.description(), request.montant(), request.utilisateurId())
        );
    }

    /**
     * Liquide un engagement budgétaire (facturation réelle).
     */
    @PostMapping("/engagements/{numero}/liquider")
    public void liquider(@PathVariable String numero, @RequestParam UUID userId) {
        liquiderEngagementUseCase.execute(new LiquiderEngagementUseCase.Input(numero, userId));
    }

    /**
     * Annule un engagement budgétaire.
     */
    @PostMapping("/engagements/{numero}/annuler")
    public void annuler(@PathVariable String numero, @RequestParam UUID userId) {
        annulerEngagementUseCase.execute(new AnnulerEngagementUseCase.Input(numero, userId));
    }
}
