package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.LigneInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SaisieEcritureInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SimulationTvaInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.SimulationTvaResponse;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import com.sodepa.erp.comptabilite.generale.presentation.requests.SaisieEcritureRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.SimulationTvaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Contrôleur REST exposant les API de gestion des écritures comptables.
 */
@RestController
@RequestMapping("/api/comptabilite/ecritures")
@RequiredArgsConstructor
public class EcritureRestController {

    private final SaisirEcritureUseCase saisirEcritureUseCase;
    private final SimulerTvaUseCase simulerTvaUseCase;
    private final SoumettreEcritureUseCase soumettreEcritureUseCase;
    private final ValiderEcritureUseCase validerEcritureUseCase;
    private final RejeterEcritureUseCase rejeterEcritureUseCase;
    private final GetEcritureByIdUseCase getEcritureByIdUseCase;

    /**
     * Saisit une nouvelle écriture comptable.
     *
     * @param request la requête de saisie
     * @return l'écriture créée
     */
    @PostMapping
    public EcritureOutput saisirEcriture(@Valid @RequestBody SaisieEcritureRequest request) {
        SaisieEcritureInput input = new SaisieEcritureInput(
                request.journalId(),
                request.numeroPiece(),
                request.libelle(),
                request.dateComptable(),
                request.typeDevise(),
                request.tauxChange(),
                request.lignes().stream().map(l -> new LigneInput(
                        l.compteCode(),
                        l.tiersId(),
                        l.debit(),
                        l.credit(),
                        l.libelleLigne()
                )).collect(Collectors.toList())
        );
        return saisirEcritureUseCase.execute(input);
    }

    /**
     * Simule les lignes d'écriture comptable pour une opération soumise à la TVA.
     *
     * @param request la requête de simulation
     * @return la réponse de simulation
     */
    @PostMapping("/simuler-tva")
    public SimulationTvaResponse simulerTva(@Valid @RequestBody SimulationTvaRequest request) {
        SimulationTvaInput input = new SimulationTvaInput(
                request.montantHt(),
                request.tauxTva(),
                request.compteHtCode()
        );
        return simulerTvaUseCase.execute(input);
    }

    /**
     * Soumet une écriture brouillon pour validation.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture soumise
     */
    @PostMapping("/{id}/soumettre")
    public EcritureOutput soumettrePourValidation(@PathVariable UUID id) {
        return soumettreEcritureUseCase.execute(id);
    }

    /**
     * Valide définitivement une écriture soumise.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture validée
     */
    @PostMapping("/{id}/valider")
    public EcritureOutput validerEcriture(@PathVariable UUID id) {
        return validerEcritureUseCase.execute(id);
    }

    /**
     * Rejette une écriture soumise.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture rejetée
     */
    @PostMapping("/{id}/rejeter")
    public EcritureOutput rejeterEcriture(@PathVariable UUID id) {
        return rejeterEcritureUseCase.execute(id);
    }

    /**
     * Récupère une écriture par son identifiant.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture
     */
    @GetMapping("/{id}")
    public EcritureOutput getEcritureById(@PathVariable UUID id) {
        return getEcritureByIdUseCase.execute(id);
    }
}
