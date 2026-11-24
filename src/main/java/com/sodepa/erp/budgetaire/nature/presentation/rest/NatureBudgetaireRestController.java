package com.sodepa.erp.budgetaire.nature.presentation.rest;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.commun.ChangerEtatRequest;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeRequest;
import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.application.inputs.CreerNatureInput;
import com.sodepa.erp.budgetaire.nature.application.inputs.ListerNaturesInput;
import com.sodepa.erp.budgetaire.nature.application.inputs.ModifierNatureInput;
import com.sodepa.erp.budgetaire.nature.application.outputs.NatureBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.application.usecase.ConsulterNatureUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.CreerNatureUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.DeciderDemandeNatureUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.DesactiverNatureUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.ListerDemandesNatureEnAttenteUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.ListerNaturesUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.ModifierNatureUseCase;
import com.sodepa.erp.budgetaire.nature.application.usecase.ReactiverNatureUseCase;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import com.sodepa.erp.budgetaire.nature.presentation.requests.CreerNatureRequest;
import com.sodepa.erp.budgetaire.nature.presentation.requests.ModifierNatureRequest;
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
 * Natures budgétaires : la classe d'une ligne, avec sa section, son sens et
 * son compte SYSCOHADA (F-BUD-026, US-BUD-108).
 *
 * <p>Aucune logique ici : chaque route convertit sa requête et délègue à
 * {@code NatureBudgetaireAdapter}. Toute écriture répond 202.</p>
 */
@RestController
@RequestMapping("/api/budget/natures")
@RequiredArgsConstructor
@Tag(name = "Budget — natures", description = "Natures des lignes budgétaires et leur compte SYSCOHADA")
public class NatureBudgetaireRestController {

    private final CreerNatureUseCase creerNatureUseCase;
    private final ModifierNatureUseCase modifierNatureUseCase;
    private final DesactiverNatureUseCase desactiverNatureUseCase;
    private final ReactiverNatureUseCase reactiverNatureUseCase;
    private final DeciderDemandeNatureUseCase deciderDemandeNatureUseCase;
    private final ListerNaturesUseCase listerNaturesUseCase;
    private final ConsulterNatureUseCase consulterNatureUseCase;
    private final ListerDemandesNatureEnAttenteUseCase listerDemandesNatureEnAttenteUseCase;

    /** Demande la création d'une nature. */
    @Operation(summary = "Soumettre la création d'une nature")
    @PostMapping("/init_create")
    public ResponseEntity<SubmissionOutput> initCreer(@Valid @RequestBody CreerNatureRequest request) {
        return ResponseEntity.accepted().body(creerNatureUseCase.execute(new CreerNatureInput(
                request.code(), request.libelle(), request.section(), request.sens(), request.compteCode())));
    }

    /** Demande le changement de libellé. */
    @Operation(summary = "Soumettre la modification d'une nature")
    @PostMapping("/{id}/init_update")
    public ResponseEntity<SubmissionOutput> initModifier(@PathVariable UUID id,
                                                         @Valid @RequestBody ModifierNatureRequest request) {
        return ResponseEntity.accepted().body(modifierNatureUseCase.execute(new ModifierNatureInput(id, request.libelle())));
    }

    /** Demande la désactivation ; le motif est obligatoire. */
    @Operation(summary = "Soumettre la désactivation d'une nature")
    @PostMapping("/{id}/init_deactivate")
    public ResponseEntity<SubmissionOutput> initDesactiver(@PathVariable UUID id,
                                                           @Valid @RequestBody ChangerEtatRequest request) {
        return ResponseEntity.accepted().body(desactiverNatureUseCase.execute(new ChangerEtatInput(id, request.motif())));
    }

    /** Demande la réactivation ; le corps et le motif sont facultatifs. */
    @Operation(summary = "Soumettre la réactivation d'une nature")
    @PostMapping("/{id}/init_reactivate")
    public ResponseEntity<SubmissionOutput> initReactiver(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ChangerEtatRequest request) {
        return ResponseEntity.accepted().body(reactiverNatureUseCase.execute(
                new ChangerEtatInput(id, request == null ? null : request.motif())));
    }

    /**
     * Valide ou rejette une demande.
     *
     * @param requestId identifiant de la <strong>demande</strong>, pas de la nature
     */
    @Operation(summary = "Valider ou rejeter une demande portant sur une nature")
    @PostMapping("/validate_or_reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void decider(@PathVariable UUID requestId, @Valid @RequestBody DeciderDemandeRequest request) {
        deciderDemandeNatureUseCase.execute(new DeciderDemandeInput(requestId, request.decision(), request.notes()));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues). */
    @Operation(summary = "Demandes sur les natures en attente de décision")
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<DemandeBudgetaireOutput<NatureBudgetaireOutput>> demandesEnAttente(
            @PageableDefault(size = 20) Pageable pageable) {
        return listerDemandesNatureEnAttenteUseCase.execute(pageable);
    }

    /** Natures triées par code, filtrables par section et par sens. */
    @Operation(summary = "Lister les natures budgétaires")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<NatureBudgetaireOutput> lister(@PageableDefault(size = 20) Pageable pageable,
                                                     @RequestParam(required = false) SectionBudgetaire section,
                                                     @RequestParam(required = false) SensBudgetaire sens,
                                                     @RequestParam(defaultValue = "false") boolean inclureInactives) {
        return listerNaturesUseCase.execute(new ListerNaturesInput(pageable, section, sens, inclureInactives));
    }

    /** Fiche d'une nature. */
    @Operation(summary = "Consulter une nature budgétaire")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public NatureBudgetaireOutput consulter(@PathVariable UUID id) {
        return consulterNatureUseCase.execute(id);
    }
}
