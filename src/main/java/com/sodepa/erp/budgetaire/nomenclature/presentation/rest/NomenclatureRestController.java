package com.sodepa.erp.budgetaire.nomenclature.presentation.rest;

import com.sodepa.erp.budgetaire.commun.DeciderDemandeRequest;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ChangerEtatElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.CreerElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.DeciderDemandeNomenclatureInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ModifierElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.outputs.ElementProgrammatiqueOutput;
import com.sodepa.erp.budgetaire.nomenclature.application.outputs.NoeudNomenclatureOutput;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.ConsulterArbreNomenclatureUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.ConsulterElementUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.CreerElementUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.DeciderDemandeNomenclatureUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.DesactiverElementUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.ListerDemandesNomenclatureEnAttenteUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.ModifierElementUseCase;
import com.sodepa.erp.budgetaire.nomenclature.application.usecase.ReactiverElementUseCase;
import com.sodepa.erp.budgetaire.nomenclature.presentation.requests.ChangerEtatElementRequest;
import com.sodepa.erp.budgetaire.nomenclature.presentation.requests.CreerElementRequest;
import com.sodepa.erp.budgetaire.nomenclature.presentation.requests.ModifierElementRequest;
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

import java.util.List;
import java.util.UUID;

/**
 * Nomenclature budgétaire : programmes et actions (F-BUD-002, US-BUD-104).
 *
 * <p>
 * Aucune logique ici : chaque route convertit sa requête et délègue. Droits,
 * rattachements et séparation maker-checker sont contrôlés par
 * {@code NomenclatureAdapter}. Toute écriture répond 202 : elle n'est
 * appliquée qu'après validation par un second utilisateur.
 * </p>
 */
@RestController
@RequestMapping("/api/budget/nomenclature")
@RequiredArgsConstructor
@Tag(name = "Budget — nomenclature", description = "Programmes et actions du budget-programme")
public class NomenclatureRestController {

    private final CreerElementUseCase creerElementUseCase;
    private final ModifierElementUseCase modifierElementUseCase;
    private final DesactiverElementUseCase desactiverElementUseCase;
    private final ReactiverElementUseCase reactiverElementUseCase;
    private final DeciderDemandeNomenclatureUseCase deciderDemandeNomenclatureUseCase;
    private final ConsulterArbreNomenclatureUseCase consulterArbreNomenclatureUseCase;
    private final ConsulterElementUseCase consulterElementUseCase;
    private final ListerDemandesNomenclatureEnAttenteUseCase listerDemandesNomenclatureEnAttenteUseCase;

    /** Demande la création d'un programme ou d'une action. */
    @Operation(summary = "Soumettre la création d'un programme ou d'une action")
    @PostMapping("/init_create")
    public ResponseEntity<SubmissionOutput> initCreer(@Valid @RequestBody CreerElementRequest request) {
        return ResponseEntity.accepted().body(creerElementUseCase.execute(new CreerElementInput(
                request.niveau(), request.code(), request.libelle(), request.description(),
                request.parentId(), request.responsableId())));
    }

    /** Demande la modification du libellé, de la description ou du responsable. */
    @Operation(summary = "Soumettre la modification d'un élément")
    @PostMapping("/{id}/init_update")
    public ResponseEntity<SubmissionOutput> initModifier(@PathVariable UUID id,
                                                         @Valid @RequestBody ModifierElementRequest request) {
        return ResponseEntity.accepted().body(modifierElementUseCase.execute(new ModifierElementInput(
                id, request.libelle(), request.description(), request.responsableId())));
    }

    /** Demande la désactivation d'un élément ; le motif est obligatoire. */
    @Operation(summary = "Soumettre la désactivation d'un élément")
    @PostMapping("/{id}/init_deactivate")
    public ResponseEntity<SubmissionOutput> initDesactiver(@PathVariable UUID id,
                                                           @Valid @RequestBody ChangerEtatElementRequest request) {
        return ResponseEntity.accepted().body(
                desactiverElementUseCase.execute(new ChangerEtatElementInput(id, request.motif())));
    }

    /** Demande la réactivation d'un élément désactivé ; le corps et le motif sont facultatifs. */
    @Operation(summary = "Soumettre la réactivation d'un élément")
    @PostMapping("/{id}/init_reactivate")
    public ResponseEntity<SubmissionOutput> initReactiver(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ChangerEtatElementRequest request) {
        return ResponseEntity.accepted().body(reactiverElementUseCase.execute(
                new ChangerEtatElementInput(id, request == null ? null : request.motif())));
    }

    /**
     * Valide ou rejette une demande.
     *
     * @param requestId identifiant de la <strong>demande</strong>, pas de l'élément
     */
    @Operation(summary = "Valider ou rejeter une demande portant sur la nomenclature")
    @PostMapping("/validate_or_reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void decider(@PathVariable UUID requestId, @Valid @RequestBody DeciderDemandeRequest request) {
        deciderDemandeNomenclatureUseCase.execute(
                new DeciderDemandeNomenclatureInput(requestId, request.decision(), request.notes()));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues). */
    @Operation(summary = "Demandes sur la nomenclature en attente de décision")
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<MakerCheckerSmartOutput> demandesEnAttente(@PageableDefault(size = 20) Pageable pageable) {
        return listerDemandesNomenclatureEnAttenteUseCase.execute(pageable);
    }

    /** L'arbre complet, programmes en tête. Non paginé : voir {@code NomenclatureAdapter#arbre}. */
    @Operation(summary = "Arbre de la nomenclature budgétaire")
    @GetMapping("/arbre")
    @ResponseStatus(HttpStatus.OK)
    public List<NoeudNomenclatureOutput> arbre(@RequestParam(defaultValue = "false") boolean inclureInactifs) {
        return consulterArbreNomenclatureUseCase.execute(inclureInactifs);
    }

    /** Fiche d'un élément. */
    @Operation(summary = "Consulter un élément de la nomenclature")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ElementProgrammatiqueOutput consulter(@PathVariable UUID id) {
        return consulterElementUseCase.execute(id);
    }
}
