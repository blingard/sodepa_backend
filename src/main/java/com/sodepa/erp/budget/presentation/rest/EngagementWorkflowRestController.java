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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST gérant le circuit d'approbation et de validation multiniveaux des engagements budgétaires.
 * <p>
 * Permet aux ordonnateurs et comptables d'effectuer les pré-engagements, de valider les visas budgétaires successifs
 * et de rejeter les dossiers non conformes avec libération immédiate des enveloppes de crédit.
 * </p>
 */
@RestController
@RequestMapping("/api/budget/engagements/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow Budgétaire", description = "Endpoints de gestion du circuit de validation multiniveaux des engagements de dépenses")
public class EngagementWorkflowRestController {

    /** Cas d'utilisation pour la soumission d'une réservation budgétaire préalable. */
    private final SoumettrePreEngagementUseCase soumettrePreEngagementUseCase;

    /** Cas d'utilisation pour le visa et la validation d'une étape de l'engagement. */
    private final ValiderEtapeWorkflowUseCase validerEtapeWorkflowUseCase;

    /** Cas d'utilisation pour le rejet d'un engagement et la désallocation des fonds. */
    private final RejeterEngagementUseCase rejeterEngagementUseCase;

    /**
     * Soumet un nouveau pré-engagement de dépense (bon de commande temporaire avec réservation de crédit).
     *
     * @param request Paramètres du pré-engagement (plan, compte, section, montant, description, utilisateur).
     * @return DTO contenant les informations de l'engagement créé et son statut initial.
     */
    @PostMapping("/pre-engager")
    @Operation(summary = "Créer un pré-engagement de dépense avec réservation de crédit budgétaire")
    public BudgetEngagementOutput preEngager(@Valid @RequestBody PreEngagementRequest request) {
        return soumettrePreEngagementUseCase.execute(
                new PreEngagementInput(request.planId(), request.compteCode(), request.sectionId(), request.numeroEngagement(), request.description(), request.montant(), request.utilisateurId())
        );
    }

    /**
     * Applique la validation d'une étape par un approbateur habilité (visa Contrôleur financier, DAF, etc.).
     *
     * @param request Paramètres de validation contenant le numéro d'engagement, le rôle et l'utilisateur.
     */
    @PostMapping("/valider")
    @Operation(summary = "Valider une étape du circuit d'engagement budgétaire")
    public void validerEtape(@Valid @RequestBody ValiderEtapeRequest request) {
        validerEtapeWorkflowUseCase.execute(
                new ValiderEtapeInput(request.numeroEngagement(), request.roleApprobateur(), request.utilisateurId())
        );
    }

    /**
     * Rejette définitivement un engagement budgétaire et libère immédiatement l'enveloppe réservée.
     *
     * @param request Paramètres de rejet contenant le numéro d'engagement, le motif et l'utilisateur.
     */
    @PostMapping("/rejeter")
    @Operation(summary = "Rejeter un engagement budgétaire et restituer les crédits réservés")
    public void rejeter(@Valid @RequestBody RejeterRequest request) {
        rejeterEngagementUseCase.execute(
                new RejeterInput(request.numeroEngagement(), request.motif(), request.utilisateurId())
        );
    }
}
