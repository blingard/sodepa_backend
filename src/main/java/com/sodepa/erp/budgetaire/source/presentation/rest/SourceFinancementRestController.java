package com.sodepa.erp.budgetaire.source.presentation.rest;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.commun.ChangerEtatRequest;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeRequest;
import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.source.application.inputs.CreerSourceInput;
import com.sodepa.erp.budgetaire.source.application.inputs.ListerSourcesInput;
import com.sodepa.erp.budgetaire.source.application.inputs.ModifierSourceInput;
import com.sodepa.erp.budgetaire.source.application.outputs.SourceFinancementOutput;
import com.sodepa.erp.budgetaire.source.application.usecase.ConsulterSourceUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.CreerSourceUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.DeciderDemandeSourceUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.DesactiverSourceUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.ListerDemandesSourceEnAttenteUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.ListerSourcesUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.ModifierSourceUseCase;
import com.sodepa.erp.budgetaire.source.application.usecase.ReactiverSourceUseCase;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import com.sodepa.erp.budgetaire.source.presentation.requests.CreerSourceRequest;
import com.sodepa.erp.budgetaire.source.presentation.requests.ModifierSourceRequest;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.PageRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Sources de financement : l'origine des fonds d'une ligne (F-BUD-026, US-BUD-108).
 *
 * <p>Aucune logique ici : chaque route convertit sa requête et délègue à
 * {@code SourceFinancementAdapter}. Toute écriture répond 202.</p>
 */
@RestController
@RequestMapping("/api/budget/sources-financement")
@RequiredArgsConstructor
@Tag(name = "Budget — sources de financement", description = "Origine des fonds des lignes budgétaires")
public class SourceFinancementRestController {

    private final CreerSourceUseCase creerSourceUseCase;
    private final ModifierSourceUseCase modifierSourceUseCase;
    private final DesactiverSourceUseCase desactiverSourceUseCase;
    private final ReactiverSourceUseCase reactiverSourceUseCase;
    private final DeciderDemandeSourceUseCase deciderDemandeSourceUseCase;
    private final ListerSourcesUseCase listerSourcesUseCase;
    private final ConsulterSourceUseCase consulterSourceUseCase;
    private final ListerDemandesSourceEnAttenteUseCase listerDemandesSourceEnAttenteUseCase;

    /** Demande la création d'une source. */
    @Operation(summary = "Soumettre la création d'une source de financement")
    @PostMapping("/init_create")
    public ResponseEntity<SubmissionOutput> initCreer(@Valid @RequestBody CreerSourceRequest request) {
        return ResponseEntity.accepted().body(creerSourceUseCase.execute(new CreerSourceInput(
                request.code(), request.libelle(), request.type(), request.bailleur())));
    }

    /** Demande le changement de libellé ou de bailleur. */
    @Operation(summary = "Soumettre la modification d'une source de financement")
    @PostMapping("/{id}/init_update")
    public ResponseEntity<SubmissionOutput> initModifier(@PathVariable UUID id,
                                                         @Valid @RequestBody ModifierSourceRequest request) {
        return ResponseEntity.accepted().body(modifierSourceUseCase.execute(
                new ModifierSourceInput(id, request.libelle(), request.bailleur())));
    }

    /** Demande la désactivation ; le motif est obligatoire. */
    @Operation(summary = "Soumettre la désactivation d'une source de financement")
    @PostMapping("/{id}/init_deactivate")
    public ResponseEntity<SubmissionOutput> initDesactiver(@PathVariable UUID id,
                                                           @Valid @RequestBody ChangerEtatRequest request) {
        return ResponseEntity.accepted().body(desactiverSourceUseCase.execute(new ChangerEtatInput(id, request.motif())));
    }

    /** Demande la réactivation ; le corps et le motif sont facultatifs. */
    @Operation(summary = "Soumettre la réactivation d'une source de financement")
    @PostMapping("/{id}/init_reactivate")
    public ResponseEntity<SubmissionOutput> initReactiver(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ChangerEtatRequest request) {
        return ResponseEntity.accepted().body(reactiverSourceUseCase.execute(
                new ChangerEtatInput(id, request == null ? null : request.motif())));
    }

    /**
     * Valide ou rejette une demande.
     *
     * @param requestId identifiant de la <strong>demande</strong>, pas de la source
     */
    @Operation(summary = "Valider ou rejeter une demande portant sur une source de financement")
    @PostMapping("/validate_or_reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void decider(@PathVariable UUID requestId, @Valid @RequestBody DeciderDemandeRequest request) {
        deciderDemandeSourceUseCase.execute(new DeciderDemandeInput(requestId, request.decision(), request.notes()));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues). */
    @Operation(summary = "Demandes sur les sources de financement en attente de décision")
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<DemandeBudgetaireOutput<SourceFinancementOutput>> demandesEnAttente(
            @PageableDefault(size = 20) Pageable pageable) {
        return listerDemandesSourceEnAttenteUseCase.execute(pageable);
    }

    /** Sources triées par code, filtrables par type. */
    @Operation(summary = "Lister les sources de financement")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<SourceFinancementOutput> lister(@PageableDefault(size = 20) Pageable pageable,
                                                      @RequestParam(required = false) TypeSourceFinancement type,
                                                      @RequestParam(defaultValue = "false") boolean inclureInactives) {
        return listerSourcesUseCase.execute(new ListerSourcesInput(pageable, type, inclureInactives));
    }

    /** Fiche d'une source. */
    @Operation(summary = "Consulter une source de financement")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SourceFinancementOutput consulter(@PathVariable UUID id) {
        return consulterSourceUseCase.execute(id);
    }
}
