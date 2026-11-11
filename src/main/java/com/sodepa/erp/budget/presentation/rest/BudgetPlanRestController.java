package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.AjouterItemInput;
import com.sodepa.erp.budget.application.inputs.CreerBudgetPlanInput;
import com.sodepa.erp.budget.application.inputs.EngagementInput;
import com.sodepa.erp.budget.application.inputs.ReallocationInput;
import com.sodepa.erp.budget.application.inputs.RechercheBudgetPlanInput;
import com.sodepa.erp.budget.application.inputs.RechercheEngagementInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.application.outputs.BudgetItemOutput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanOutput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanSmartOutput;
import com.sodepa.erp.budget.application.usecase.*;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.budget.infrastructure.entities.StatutBudget;
import com.sodepa.erp.budget.presentation.requests.AjouterItemRequest;
import com.sodepa.erp.budget.presentation.requests.CreerBudgetRequest;
import com.sodepa.erp.budget.presentation.requests.EngagementRequest;
import com.sodepa.erp.budget.presentation.requests.ReallocationRequest;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.share.RejectOrValidateSubmitRequest;
import com.sodepa.erp.utils.PageRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Point d'entree principal pour la gestion budgetaire de la SODEPA (Module 02 - BUD).
 * <p>
 * Ce service permet de gerer tout le cycle de vie d'un budget annuel :
 * <ul>
 *   <li>L'elaboration et la saisie des previsions initiales par programme et par unite.</li>
 *   <li>La soumission et l'approbation institutionnelle (Direction Generale et Conseil d'Administration).</li>
 *   <li>La reservation et l'engagement effectif des depenses (avec controle bloquant de depassement).</li>
 *   <li>La liquidation des engagements lors de la reception des factures et l'annulation en cas d'imprevu.</li>
 *   <li>Le virement ou la reallocation d'enveloppes budgetaires entre differentes rubriques.</li>
 * </ul>
 * Il applique de maniere stricte les regles d'autorisation, de separation des taches et de cloisonnement
 * geographique (entre le siege de Yaounde et les ranches/abattoirs decentralises).
 */
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@Tag(name = "Gestion Budgétaire Principale", description = "Opérations sur les plans annuels, engagements de dépenses et réallocations")
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
    private final GetPageBudgetPlansUseCase getPageBudgetPlansUseCase;
    private final GetBudgetPlanByIdUseCase getBudgetPlanByIdUseCase;
    private final GetPageEngagementsUseCase getPageEngagementsUseCase;
    private final GetEngagementByNumeroUseCase getEngagementByNumeroUseCase;
    private final BudgetPlanAdapter budgetPlanAdapter;

    /**
     * Recherche et liste les budgets annuels enregistres dans l'ERP avec pagination.
     * <p>
     * Permet aux responsables financiers d'avoir une vue globale sur les budgets passes,
     * en cours de preparation ou en execution.
     *
     * @param pageable parametres de pagination et de tri
     * @param annee filtre optionnel sur une annee d'exercice precise (ex: 2026)
     * @param statut filtre optionnel sur l'etat d'avancement du budget (DRAFT, VOTE, EXECUTOIRE, etc.)
     * @return une page contenant le resume des budgets correspondants aux criteres
     */
    @GetMapping("/plans")
    @Operation(summary = "Rechercher des plans budgétaires annuels")
    public PageRecord<BudgetPlanSmartOutput> listerPlans(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) StatutBudget statut
    ) {
        return getPageBudgetPlansUseCase.execute(new RechercheBudgetPlanInput(pageable, annee, statut));
    }

    /**
     * Consulte le detail complet d'un budget annuel specifique.
     * <p>
     * Renvoie la liste de toutes les lignes budgetaires associees, les dotations annuelles votees,
     * les montants deja engages, les realisations comptables et le montant encore disponible.
     *
     * @param planId identifiant unique du budget demande
     * @return le detail exhaustif du plan et de ses lignes de depenses
     */
    @GetMapping("/plans/{planId}")
    @Operation(summary = "Consulter le détail complet d'un plan budgétaire")
    public BudgetPlanOutput getPlan(@PathVariable UUID planId) {
        return getBudgetPlanByIdUseCase.execute(planId);
    }

    /**
     * Liste l'ensemble des bons d'engagements de depenses avec filtrage de securite.
     * <p>
     * Seules les depenses correspondant au perimetre autorise de l'agent connecte sont renvoyees :
     * les agents du siege ont une vue consolidée sur toute la societe, tandis que les agents
     * en unite operationnelle (ranches de Faro/Dumbo, abattoirs) ne visualisent que les engagements de leur site.
     *
     * @param pageable informations de pagination (page, taille, tri)
     * @param planId filtre optionnel pour cibler un exercice budgetaire precis
     * @param statut filtre optionnel sur l'etat du bon (ENGAGED, CONVERTED_TO_REAL, CANCELLED)
     * @return une liste paginee des bons d'engagements correspondants
     */
    @GetMapping("/engagements")
    @Operation(summary = "Lister les engagements budgétaires selon le périmètre de l'utilisateur")
    public PageRecord<BudgetEngagementOutput> listerEngagements(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) UUID planId,
            @RequestParam(required = false) String statut
    ) {
        return getPageEngagementsUseCase.execute(new RechercheEngagementInput(pageable, planId, statut));
    }

    /**
     * Recherche et affiche un bon d'engagement de depense a partir de sa reference unique.
     *
     * @param numero reference officielle du bon d'engagement (ex: BE-2026-HQ-001)
     * @return le detail complet de l'engagement (montant, ligne budgetaire, circuit, visa appose)
     */
    @GetMapping("/engagements/{numero}")
    @Operation(summary = "Consulter un engagement par sa référence unique")
    public BudgetEngagementOutput getEngagement(@PathVariable String numero) {
        return getEngagementByNumeroUseCase.execute(numero);
    }

    /**
     * Initialise un nouvel exercice budgetaire annuel a l'etat de brouillon.
     * <p>
     * Cette action marque le point de depart de la confection annuelle du budget (Projet de Performance Annuel).
     *
     * @param request informations de creation comprenant l'annee cible, le libelle et l'auteur
     * @return le resume du budget cree a l'etat 'EN_PREPARATION' ou 'DRAFT'
     */
    @PostMapping("/plans")
    @Operation(summary = "Créer un nouveau budget annuel (Brouillon / Préparation)")
    public BudgetPlanOutput creerBudgetPlan(@Valid @RequestBody CreerBudgetRequest request) {
        return creerBudgetPlanUseCase.execute(
                new CreerBudgetPlanInput(request.annee(), request.intitule(), request.utilisateurId())
        );
    }

    /**
     * Ajoute une ligne budgetaire individualisee (rubrique de depense) au sein d'un budget.
     * <p>
     * Chaque ligne est rattachee a un compte du plan comptable et a une section organisationnelle
     * (direction, ranch, station d'elevage, abattoir).
     *
     * @param planId identifiant du budget concerne
     * @param request parametres de la ligne (compte comptable, section/unite, dotation annuelle prevue)
     * @return la ligne budgetaire creee avec sa repartition mensuelle
     */
    @PostMapping("/plans/{planId}/items")
    @Operation(summary = "Ajouter une ligne de dépense détaillée au budget")
    public BudgetItemOutput ajouterItem(@PathVariable UUID planId, @Valid @RequestBody AjouterItemRequest request) {
        return ajouterItemPlanUseCase.execute(
                new AjouterItemInput(planId, request.compteCode(), request.sectionId(), request.montant())
        );
    }

    /**
     * Soumet un budget finalise pour instruction et validation institutionnelle.
     * <p>
     * Enclenche le processus hierarchique formalise (examen par le groupe de travail et transmission au DAF).
     *
     * @param planId identifiant du budget a soumettre
     * @param userId identifiant du gestionnaire effectuant la soumission
     */
    @PostMapping("/plans/{planId}/soumettre")
    @Operation(summary = "Soumettre le projet de budget au circuit de validation")
    public void soumettrePlan(@PathVariable UUID planId, @RequestParam UUID userId) {
        soumettrePlanUseCase.execute(new SoumettrePlanUseCase.Input(planId, userId));
    }

    /**
     * Valide et officialise l'adoption definitive d'un budget annuel.
     * <p>
     * Rend le budget executoire (autorisant le lancement des depenses et la passation des marches).
     *
     * @param planId identifiant du budget adopte
     * @param userId identifiant de l'autorite validatrice
     */
    @PostMapping("/plans/{planId}/approuver")
    @Operation(summary = "Approuver et rendre exécutoire le budget")
    public void approuverPlan(@PathVariable UUID planId, @RequestParam UUID userId) {
        approuverPlanUseCase.execute(new ApprouverPlanUseCase.Input(planId, userId));
    }

    /**
     * Rejette un projet de budget presente, avec renvoi pour corrections ou ajustements d'arbitrage.
     *
     * @param planId identifiant du budget refuse
     * @param userId identifiant de l'autorite ayant pris la decision de rejet
     */
    @PostMapping("/plans/{planId}/rejeter")
    @Operation(summary = "Rejeter le projet de budget annuel")
    public void rejeterPlan(@PathVariable UUID planId, @RequestParam UUID userId) {
        rejeterPlanUseCase.execute(new RejeterPlanUseCase.Input(planId, userId));
    }

    /**
     * Opere un transfert de credits (reallocation ou virement) entre deux lignes budgetaires.
     * <p>
     * Selon l'ampleur du transfert et l'appartenance ou non a la meme unite :
     * <ul>
     *   <li>Une reallocation simple (&le; 5% de la dotation) est validee au niveau du DAF.</li>
     *   <li>Une reallocation significative (> 5% ou inter-sites) requiert un comite d'analyse et une resolution du Conseil d'Administration.</li>
     *   <li>Une mesure d'urgence permet au Directeur General d'autoriser l'operation a titre conservatoire.</li>
     * </ul>
     *
     * @param request parametres du virement (ligne source debitee, ligne cible creditee, montant, motif et justificatifs)
     */
    @PostMapping("/reallocations")
    @Operation(summary = "Effectuer une réallocation ou virement de crédits entre rubriques")
    public void reallocer(@Valid @RequestBody ReallocationRequest request) {
        effectuerReallocationUseCase.execute(
                new ReallocationInput(request.sourceItemId(), request.destItemId(), request.montant(), request.responsableId(), request.raison())
        );
    }

    /**
     * Enregistre une reservation formelle de credits (Bon d'Engagement de depense).
     * <p>
     * Realise des verifications de securite financiere strictes :
     * <ul>
     *   <li>Controle que le budget de l'exercice est bien en statut executoire.</li>
     *   <li>Controle bloquant du solde disponible (refus immediat de tout depassement).</li>
     *   <li>Aiguillage automatique entre circuit de Menue Depense (&lt; 500 000 FCFA) et Grosse Depense (&ge; 500 000 FCFA).</li>
     *   <li>Controle d'appartenance geographique de l'agent a l'unite emettrice.</li>
     * </ul>
     *
     * @param request donnees de l'engagement (imputation, montant, libelle de la depense)
     * @return le bon d'engagement genere et formalise
     */
    @PostMapping("/engagements")
    @Operation(summary = "Émettre et enregistrer un bon d'engagement budgétaire")
    public BudgetEngagementOutput engager(@Valid @RequestBody EngagementRequest request) {
        return enregistrerEngagementUseCase.execute(
                new EngagementInput(request.planId(), request.compteCode(), request.sectionId(), request.numeroEngagement(), request.description(), request.montant(), request.utilisateurId())
        );
    }

    /**
     * Liquide un engagement budgetaire a la suite de la realisation du service ou de la reception de la marchandise.
     * <p>
     * Transforme le montant reserve prealablement en depense reelle consommee dans la comptabilite generale.
     *
     * @param numero reference unique du bon d'engagement a liquider
     * @param userId identifiant de l'agent administratif constatant le service fait
     */
    @PostMapping("/engagements/{numero}/liquider")
    @Operation(summary = "Liquider un engagement (constatation du service fait et facture)")
    public void liquider(@PathVariable String numero, @RequestParam UUID userId) {
        liquiderEngagementUseCase.execute(new LiquiderEngagementUseCase.Input(numero, userId));
    }

    /**
     * Annule un bon d'engagement prealablement reserve.
     * <p>
     * Restitue integralement les credits reserves au solde disponible de la ligne budgetaire.
     * Exige l'enregistrement d'un motif officiel d'annulation pour la piste d'audit.
     *
     * @param numero reference unique du bon d'engagement a annuler
     * @param userId identifiant de l'autorite demandant l'annulation
     */
    @PostMapping("/engagements/{numero}/annuler")
    @Operation(summary = "Annuler un bon d'engagement et restituer les crédits budgétaires")
    public void annuler(@PathVariable String numero, @RequestParam UUID userId) {
        annulerEngagementUseCase.execute(new AnnulerEngagementUseCase.Input(numero, userId));
    }

    /**
     * Traite la validation ou le rejet d'une soumission de budget dans le cadre du workflow d'approbation.
     *
     * @param id identifiant unique du ticket de soumission
     * @param request decision d'approbation ou de rejet et observations eventuelles
     * @return reponse HTTP sans contenu indiquant le traitement de la demande
     */
    @PutMapping("/plans/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter un plan budgétaire soumis au double contrôle")
    public ResponseEntity<Void> validateOrRejectPlan(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        budgetPlanAdapter.validateOrRejectBudgetPlan(new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        ));
        return ResponseEntity.noContent().build();
    }

    /**
     * Traite la validation ou le rejet d'une demande de reallocation de credits dans le circuit a 4 yeux.
     *
     * @param id identifiant du ticket de reallocation a arbitrer
     * @param request decision (accord ou refus motive)
     * @return confirmation d'enregistrement de la decision
     */
    @PutMapping("/reallocations/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une réallocation budgétaire soumise au double contrôle")
    public ResponseEntity<Void> validateOrRejectReallocation(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        budgetPlanAdapter.validateOrRejectReallocation(new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        ));
        return ResponseEntity.noContent().build();
    }

    /**
     * Traite la decision finale sur un bon d'engagement de depense soumis a validation hierarchique.
     *
     * @param id identifiant de l'operation d'engagement
     * @param request accord d'engagement ou rejet pour non-conformite
     * @return confirmation de prise en compte de la decision
     */
    @PutMapping("/engagements/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter un engagement budgétaire soumis au double contrôle")
    public ResponseEntity<Void> validateOrRejectEngagement(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        budgetPlanAdapter.validateOrRejectEngagement(new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        ));
        return ResponseEntity.noContent().build();
    }
}
