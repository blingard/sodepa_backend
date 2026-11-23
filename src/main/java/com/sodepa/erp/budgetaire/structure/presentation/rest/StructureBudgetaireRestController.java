package com.sodepa.erp.budgetaire.structure.presentation.rest;

import com.sodepa.erp.budgetaire.commun.ChangerEtatRequest;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeRequest;
import com.sodepa.erp.budgetaire.structure.application.inputs.ChangerEtatStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.CreerStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.DeciderDemandeStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.ListerStructuresInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.ModifierStructureInput;
import com.sodepa.erp.budgetaire.structure.application.outputs.StructureBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.application.usecase.ConsulterStructureUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.CreerStructureUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.DeciderDemandeStructureUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.DesactiverStructureUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.ListerDemandesStructureEnAttenteUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.ListerStructuresUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.ModifierStructureUseCase;
import com.sodepa.erp.budgetaire.structure.application.usecase.ReactiverStructureUseCase;
import com.sodepa.erp.budgetaire.structure.presentation.requests.CreerStructureRequest;
import com.sodepa.erp.budgetaire.structure.presentation.requests.ModifierStructureRequest;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
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
 * Structures budgétaires : unités de l'organigramme qui portent un budget
 * (F-BUD-025, US-BUD-107).
 *
 * <p>
 * Aucune logique ici : chaque route convertit sa requête et délègue à
 * {@code StructureBudgetaireAdapter}. Toute écriture répond 202 : elle n'est
 * appliquée qu'après validation par un second utilisateur.
 * </p>
 */
@RestController
@RequestMapping("/api/budget/structures")
@RequiredArgsConstructor
@Tag(name = "Budget — structures", description = "Unités budgétaires, ordonnateurs et services d'engagement")
public class StructureBudgetaireRestController {

    private final CreerStructureUseCase creerStructureUseCase;
    private final ModifierStructureUseCase modifierStructureUseCase;
    private final DesactiverStructureUseCase desactiverStructureUseCase;
    private final ReactiverStructureUseCase reactiverStructureUseCase;
    private final DeciderDemandeStructureUseCase deciderDemandeStructureUseCase;
    private final ListerStructuresUseCase listerStructuresUseCase;
    private final ConsulterStructureUseCase consulterStructureUseCase;
    private final ListerDemandesStructureEnAttenteUseCase listerDemandesStructureEnAttenteUseCase;

    /** Demande la déclaration d'une unité comme structure budgétaire. */
    @Operation(summary = "Soumettre la déclaration d'une structure budgétaire")
    @PostMapping("/init_create")
    public ResponseEntity<SubmissionOutput> initCreer(@Valid @RequestBody CreerStructureRequest request) {
        return ResponseEntity.accepted().body(creerStructureUseCase.execute(new CreerStructureInput(
                request.uniteId(), request.ordonnateurPosteId(), request.serviceEngagementUniteId())));
    }

    /** Demande le changement d'ordonnateur ou de service d'engagement. */
    @Operation(summary = "Soumettre la modification d'une structure")
    @PostMapping("/{id}/init_update")
    public ResponseEntity<SubmissionOutput> initModifier(@PathVariable UUID id,
                                                         @Valid @RequestBody ModifierStructureRequest request) {
        return ResponseEntity.accepted().body(modifierStructureUseCase.execute(new ModifierStructureInput(
                id, request.ordonnateurPosteId(), request.serviceEngagementUniteId())));
    }

    /** Demande la désactivation d'une structure ; le motif est obligatoire. */
    @Operation(summary = "Soumettre la désactivation d'une structure")
    @PostMapping("/{id}/init_deactivate")
    public ResponseEntity<SubmissionOutput> initDesactiver(@PathVariable UUID id,
                                                           @Valid @RequestBody ChangerEtatRequest request) {
        return ResponseEntity.accepted().body(
                desactiverStructureUseCase.execute(new ChangerEtatStructureInput(id, request.motif())));
    }

    /** Demande la réactivation d'une structure ; le corps et le motif sont facultatifs. */
    @Operation(summary = "Soumettre la réactivation d'une structure")
    @PostMapping("/{id}/init_reactivate")
    public ResponseEntity<SubmissionOutput> initReactiver(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ChangerEtatRequest request) {
        return ResponseEntity.accepted().body(reactiverStructureUseCase.execute(
                new ChangerEtatStructureInput(id, request == null ? null : request.motif())));
    }

    /**
     * Valide ou rejette une demande.
     *
     * @param requestId identifiant de la <strong>demande</strong>, pas de la structure
     */
    @Operation(summary = "Valider ou rejeter une demande portant sur une structure")
    @PostMapping("/validate_or_reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void decider(@PathVariable UUID requestId, @Valid @RequestBody DeciderDemandeRequest request) {
        deciderDemandeStructureUseCase.execute(
                new DeciderDemandeStructureInput(requestId, request.decision(), request.notes()));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues). */
    @Operation(summary = "Demandes sur les structures en attente de décision")
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<MakerCheckerSmartOutput> demandesEnAttente(@PageableDefault(size = 20) Pageable pageable) {
        return listerDemandesStructureEnAttenteUseCase.execute(pageable);
    }

    /** Structures, de la plus ancienne déclaration à la plus récente. */
    @Operation(summary = "Lister les structures budgétaires")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<StructureBudgetaireOutput> lister(@PageableDefault(size = 20) Pageable pageable,
                                                        @RequestParam(defaultValue = "false") boolean inclureInactives) {
        return listerStructuresUseCase.execute(new ListerStructuresInput(pageable, inclureInactives));
    }

    /** Fiche d'une structure, avec le titulaire actuel du poste d'ordonnateur. */
    @Operation(summary = "Consulter une structure budgétaire")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StructureBudgetaireOutput consulter(@PathVariable UUID id) {
        return consulterStructureUseCase.execute(id);
    }
}
