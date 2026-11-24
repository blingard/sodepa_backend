package com.sodepa.erp.budgetaire.exercice.presentation.rest;

import com.sodepa.erp.budgetaire.commun.DeciderDemandeRequest;
import com.sodepa.erp.budgetaire.exercice.application.inputs.CreerExerciceInput;
import com.sodepa.erp.budgetaire.exercice.application.inputs.DeciderDemandeExerciceInput;
import com.sodepa.erp.budgetaire.exercice.application.outputs.ExerciceBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.application.usecase.ConsulterExerciceUseCase;
import com.sodepa.erp.budgetaire.exercice.application.usecase.CreerExerciceUseCase;
import com.sodepa.erp.budgetaire.exercice.application.usecase.DeciderDemandeExerciceUseCase;
import com.sodepa.erp.budgetaire.exercice.application.usecase.ListerDemandesExerciceEnAttenteUseCase;
import com.sodepa.erp.budgetaire.exercice.application.usecase.ListerExercicesUseCase;
import com.sodepa.erp.budgetaire.exercice.presentation.requests.CreerExerciceRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Exercices budgétaires (F-BUD-001).
 *
 * <p>
 * Aucune logique ici : chaque route convertit sa requête et délègue. Droits,
 * doublons et séparation maker-checker sont contrôlés par
 * {@code ExerciceBudgetaireAdapter}.
 * </p>
 */
@RestController
@RequestMapping("/api/budget/exercices")
@RequiredArgsConstructor
@Tag(name = "Budget — exercices", description = "Création et consultation des exercices budgétaires")
public class ExerciceBudgetaireRestController {

    private final CreerExerciceUseCase creerExerciceUseCase;
    private final DeciderDemandeExerciceUseCase deciderDemandeExerciceUseCase;
    private final ListerExercicesUseCase listerExercicesUseCase;
    private final ConsulterExerciceUseCase consulterExerciceUseCase;
    private final ListerDemandesExerciceEnAttenteUseCase listerDemandesExerciceEnAttenteUseCase;

    /**
     * Demande la création d'un exercice.
     *
     * <p>Répond 202 : l'exercice n'existe qu'après validation par un second
     * utilisateur. Le corps porte l'identifiant de la demande, à passer à
     * {@code validate_or_reject}, et celui de l'exercice à venir.</p>
     */
    @Operation(summary = "Soumettre la création d'un exercice budgétaire")
    @PostMapping("/init_create")
    public ResponseEntity<SubmissionOutput> initCreer(@Valid @RequestBody CreerExerciceRequest request) {
        return ResponseEntity.accepted().body(creerExerciceUseCase.execute(new CreerExerciceInput(request.annee())));
    }

    /**
     * Valide ou rejette une demande.
     *
     * @param requestId identifiant de la <strong>demande</strong>, pas de l'exercice
     */
    @Operation(summary = "Valider ou rejeter une demande portant sur un exercice")
    @PostMapping("/validate_or_reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void decider(@PathVariable UUID requestId, @Valid @RequestBody DeciderDemandeRequest request) {
        deciderDemandeExerciceUseCase.execute(
                new DeciderDemandeExerciceInput(requestId, request.decision(), request.notes()));
    }

    /** Demandes en attente que l'appelant peut trancher (les siennes exclues). */
    @Operation(summary = "Demandes sur les exercices en attente de décision")
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<MakerCheckerSmartOutput> demandesEnAttente(@PageableDefault(size = 20) Pageable pageable) {
        return listerDemandesExerciceEnAttenteUseCase.execute(pageable);
    }

    /** Exercices, du plus récent au plus ancien. */
    @Operation(summary = "Lister les exercices budgétaires")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<ExerciceBudgetaireOutput> lister(@PageableDefault(size = 20) Pageable pageable) {
        return listerExercicesUseCase.execute(pageable);
    }

    /** Un exercice, désigné par son année. */
    @Operation(summary = "Consulter un exercice budgétaire")
    @GetMapping("/{annee}")
    @ResponseStatus(HttpStatus.OK)
    public ExerciceBudgetaireOutput consulter(@PathVariable int annee) {
        return consulterExerciceUseCase.execute(annee);
    }
}
