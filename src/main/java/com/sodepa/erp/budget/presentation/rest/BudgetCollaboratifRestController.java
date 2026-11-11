package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CadrageInput;
import com.sodepa.erp.budget.application.inputs.GenererHistoriqueInput;
import com.sodepa.erp.budget.application.inputs.RechercheDemandeInput;
import com.sodepa.erp.budget.application.inputs.SaisirDemandeInput;
import com.sodepa.erp.budget.application.outputs.BudgetDemandeOutput;
import com.sodepa.erp.budget.application.usecase.*;
import com.sodepa.erp.budget.presentation.requests.CadrageRequest;
import com.sodepa.erp.budget.presentation.requests.GenererHistoriqueRequest;
import com.sodepa.erp.budget.presentation.requests.SaisirDemandeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Service dedie a l'elaboration budgetaire participative et a la navette budgetaire (Module 02 - BUD).
 * <p>
 * Ce service organise la collecte des expressions de besoins des differentes unites de la SODEPA :
 * <ul>
 *   <li>La saisie decentralisee des demandes budgetaires par les chefs de ranches, stations d'elevage et services (demarche ascendante / Bottom-Up).</li>
 *   <li>L'arbitrage hierarchique (examen, ajustement, approbation ou rejet motive).</li>
 *   <li>L'application de coefficients de cadrage macro-economiques fixes par la Direction Generale (demarche descendante / Top-Down).</li>
 *   <li>La generation automatique de previsions fondee sur l'historique reel de l'annee N-1.</li>
 *   <li>La consolidation globale des propositions retenues au sein du plan budgetaire officiel de la SODEPA.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/budget/collaboratif")
@RequiredArgsConstructor
@Tag(name = "Budget Collaboratif & Arbitrage", description = "Collecte des besoins décentralisés, navette budgétaire et consolidation")
public class BudgetCollaboratifRestController {

    private final SaisirDemandeBudgetaireUseCase saisirDemandeBudgetaireUseCase;
    private final SoumettreDemandesUseCase soumettreDemandesUseCase;
    private final ApprouverDemandeUseCase approuverDemandeUseCase;
    private final RejeterDemandeUseCase rejeterDemandeUseCase;
    private final AppliquerTauxCadrageUseCase appliquerTauxCadrageUseCase;
    private final GenererBudgetDepuisHistoriqueUseCase genererBudgetDepuisHistoriqueUseCase;
    private final ConsoliderDemandesDansPlanUseCase consoliderDemandesDansPlanUseCase;
    private final ListerDemandesBudgetairesUseCase listerDemandesBudgetairesUseCase;

    /**
     * Recherche et liste les propositions de depenses exprimees par les differentes entites.
     * <p>
     * Constitue la file d'attente d'arbitrage utilisee par le Service du Budget et la Commission Budgetaire
     * pour instruire les demandes avant leur integration dans le projet de budget.
     *
     * @param departementId filtre optionnel pour cibler un service ou une unite specifique (ex: Ranch de Dumbo)
     * @param annee exercice budgetaire prepare (ex: 2026)
     * @param statut etat d'avancement des demandes (DRAFT, SUBMITTED, APPROVED, REJECTED)
     * @return la liste des propositions budgetaires conformes aux filtres
     */
    @GetMapping("/demandes")
    @Operation(summary = "Lister les demandes budgétaires pour la commission d'arbitrage")
    public List<BudgetDemandeOutput> listerDemandes(
            @RequestParam(required = false) UUID departementId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut
    ) {
        return listerDemandesBudgetairesUseCase.execute(new RechercheDemandeInput(departementId, annee, statut));
    }

    /**
     * Enregistre un besoin de depense individuel pour une unite organisationnelle.
     * <p>
     * L'expression de besoin est initiale et conservee a l'etat de brouillon tant qu'elle n'est pas transmise
     * officiellement pour instruction.
     *
     * @param request detail du besoin (compte de charge prevu, site beneficiaire, montant estime et justification)
     * @return la proposition budgetaire enregistree
     */
    @PostMapping("/demandes")
    @Operation(summary = "Saisir une proposition budgétaire par un service (Bottom-Up)")
    public BudgetDemandeOutput saisirDemande(@Valid @RequestBody SaisirDemandeRequest request) {
        return saisirDemandeBudgetaireUseCase.execute(
                new SaisirDemandeInput(request.departementId(), request.annee(), request.compteCode(), request.sectionId(), request.montant(), request.commentaires())
        );
    }

    /**
     * Transmet l'ensemble des expressions de besoins d'une unite pour examen par la direction.
     * <p>
     * Cloture la phase de saisie pour l'unite donnee et fait basculer les demandes a l'etat 'SUBMITTED'.
     *
     * @param departementId identifiant de l'unite ou du service emetteur
     * @param annee exercice budgetaire prepare
     */
    @PostMapping("/demandes/soumettre")
    @Operation(summary = "Soumettre l'avant-projet de budget d'une unité à l'arbitrage")
    public void soumettreDemandes(@RequestParam UUID departementId, @RequestParam int annee) {
        soumettreDemandesUseCase.execute(new SoumettreDemandesUseCase.Input(departementId, annee));
    }

    /**
     * Valide et retient definitivement une proposition budgetaire lors des conferences d'arbitrage.
     *
     * @param demandeId identifiant unique de la demande examinee
     * @param userId identifiant du responsable ayant arbitre favorablement
     */
    @PostMapping("/demandes/{demandeId}/approuver")
    @Operation(summary = "Approuver une demande budgétaire lors des arbitrages")
    public void approuverDemande(@PathVariable UUID demandeId, @RequestParam UUID userId) {
        approuverDemandeUseCase.execute(new ApprouverDemandeUseCase.Input(demandeId, userId));
    }

    /**
     * Rejette une demande budgetaire jugee non prioritaire ou excessive.
     * <p>
     * Exige l'enregistrement obligatoire d'un motif de refus pour notifier et informer le service demandeur.
     *
     * @param demandeId identifiant de la demande refusee
     * @param motif explication explicite du rejet ou de l'ajournement
     * @param userId identifiant du gestionnaire ou de l'autorite ayant tranche
     */
    @PostMapping("/demandes/{demandeId}/rejeter")
    @Operation(summary = "Rejeter une demande budgétaire avec motivation obligatoire")
    public void rejeterDemande(@PathVariable UUID demandeId, @RequestParam String motif, @RequestParam UUID userId) {
        rejeterDemandeUseCase.execute(new RejeterDemandeUseCase.Input(demandeId, motif, userId));
    }

    /**
     * Applique un coefficient d'ajustement forfaitaire a l'ensemble des demandes d'une categorie de depenses.
     * <p>
     * Outil de cadrage macro-budgetaire (ex: augmentation globale de 3% sur les carburants ou baisse de 5% sur les fournitures).
     *
     * @param request parametres du cadrage (prefixe des comptes cibles, coefficient multiplicateur et responsable)
     */
    @PostMapping("/cadrage")
    @Operation(summary = "Appliquer un coefficient de cadrage budgétaire (Top-Down)")
    public void appliquerCadrage(@Valid @RequestBody CadrageRequest request) {
        appliquerTauxCadrageUseCase.execute(
                new CadrageInput(request.annee(), request.comptePrefix(), request.coefficient(), request.responsableId())
        );
    }

    /**
     * Genere automatiquement un jeu complet de previsions a partir des realisations de l'exercice precedent.
     * <p>
     * Permet d'initialiser rapidement la base de travail de l'annee N+1 a partir des donnees reelles de l'annee N-1
     * en leur appliquant des hypotheses d'evolution sur les produits et les charges d'exploitation.
     *
     * @param request parametres de projection (annee de reference, annee cible et coefficients d'indexation)
     */
    @PostMapping("/generer")
    @Operation(summary = "Pré-remplir les demandes budgétaires depuis les réalisations réelles N-1")
    public void genererDepuisHistorique(@Valid @RequestBody GenererHistoriqueRequest request) {
        genererBudgetDepuisHistoriqueUseCase.execute(
                new GenererHistoriqueInput(request.anneeSource(), request.anneeCible(), request.coeffVentes(), request.coeffCharges(), request.departementId())
        );
    }

    /**
     * Agrege l'ensemble des demandes validees de toutes les unites dans le budget officiel consolidé de la SODEPA.
     * <p>
     * Constitue l'etape finale de la navette budgetaire avant transmission du document unique au Directeur General.
     *
     * @param annee exercice budgetaire consolide
     * @param planId identifiant du plan budgetaire receptif
     * @param userId identifiant du responsable de la consolidation (Chef Service Budget)
     */
    @PostMapping("/consolider")
    @Operation(summary = "Consolider l'ensemble des besoins validés dans le budget officiel")
    public void consolider(@RequestParam int annee, @RequestParam UUID planId, @RequestParam UUID userId) {
        consoliderDemandesDansPlanUseCase.execute(new ConsoliderDemandesDansPlanUseCase.Input(annee, planId, userId));
    }
}
