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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour l'élaboration budgétaire collaborative et la navette budgétaire.
 */
@RestController
@RequestMapping("/api/budget/collaboratif")
@RequiredArgsConstructor
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
     * Liste les propositions budgétaires, filtrées au besoin.
     *
     * <p>
     * C'est la file d'arbitrage de la direction : sans elle, une demande
     * n'était visible qu'à l'instant de sa saisie.
     * </p>
     *
     * @param departementId restreint à un service, facultatif
     * @param annee restreint à un exercice, facultatif
     * @param statut restreint à un état, facultatif
     */
    @GetMapping("/demandes")
    public List<BudgetDemandeOutput> listerDemandes(
            @RequestParam(required = false) UUID departementId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut
    ) {
        return listerDemandesBudgetairesUseCase.execute(new RechercheDemandeInput(departementId, annee, statut));
    }

    /**
     * Saisit une proposition de ligne budgétaire par un service (Bottom-Up).
     */
    @PostMapping("/demandes")
    public BudgetDemandeOutput saisirDemande(@Valid @RequestBody SaisirDemandeRequest request) {
        return saisirDemandeBudgetaireUseCase.execute(
                new SaisirDemandeInput(request.departementId(), request.annee(), request.compteCode(), request.sectionId(), request.montant(), request.commentaires())
        );
    }

    /**
     * Soumet toutes les propositions budgétaires d'un service.
     */
    @PostMapping("/demandes/soumettre")
    public void soumettreDemandes(@RequestParam UUID departementId, @RequestParam int annee) {
        soumettreDemandesUseCase.execute(new SoumettreDemandesUseCase.Input(departementId, annee));
    }

    /**
     * Valide et approuve individuellement une demande budgétaire.
     */
    @PostMapping("/demandes/{demandeId}/approuver")
    public void approuverDemande(@PathVariable UUID demandeId, @RequestParam UUID userId) {
        approuverDemandeUseCase.execute(new ApprouverDemandeUseCase.Input(demandeId, userId));
    }

    /**
     * Rejette une demande budgétaire en y insérant un motif explicatif.
     */
    @PostMapping("/demandes/{demandeId}/rejeter")
    public void rejeterDemande(@PathVariable UUID demandeId, @RequestParam String motif, @RequestParam UUID userId) {
        rejeterDemandeUseCase.execute(new RejeterDemandeUseCase.Input(demandeId, motif, userId));
    }

    /**
     * Applique un taux d'ajustement global (cadrage Top-Down) sur toutes les demandes d'une classe.
     */
    @PostMapping("/cadrage")
    public void appliquerCadrage(@Valid @RequestBody CadrageRequest request) {
        appliquerTauxCadrageUseCase.execute(
                new CadrageInput(request.annee(), request.comptePrefix(), request.coefficient(), request.responsableId())
        );
    }

    /**
     * Génère automatiquement les demandes budgétaires d'un exercice d'après l'historique réel.
     */
    @PostMapping("/generer")
    public void genererDepuisHistorique(@Valid @RequestBody GenererHistoriqueRequest request) {
        genererBudgetDepuisHistoriqueUseCase.execute(
                new GenererHistoriqueInput(request.anneeSource(), request.anneeCible(), request.coeffVentes(), request.coeffCharges(), request.departementId())
        );
    }

    /**
     * Consolide toutes les propositions budgétaires APPROVED de l'année dans le plan budgétaire officiel.
     */
    @PostMapping("/consolider")
    public void consolider(@RequestParam int annee, @RequestParam UUID planId, @RequestParam UUID userId) {
        consoliderDemandesDansPlanUseCase.execute(new ConsoliderDemandesDansPlanUseCase.Input(annee, planId, userId));
    }
}
