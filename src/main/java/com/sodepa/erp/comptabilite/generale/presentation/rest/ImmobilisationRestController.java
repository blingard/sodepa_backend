package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.GenerateAmortisationInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.RechercheImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.AmortissementLineOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ImmoOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.CreateImmoUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GenerateAmortisationUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetImmoByIdUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetPageImmobilisationsUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetPendingImmoRequestsUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetPlanAmortissementUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.ImmoValidateOrRejectUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.UpdateImmoUseCase;
import com.sodepa.erp.comptabilite.generale.presentation.requests.CreateImmoRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.GenerateAmortisationRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.RejectOrValidateImmoSubmitRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.UpdateImmoRequest;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.StatutImmobilisation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des immobilisations et de leurs amortissements.
 */
@RestController
@RequestMapping("/api/v1/immobilisations")
@RequiredArgsConstructor
public class ImmobilisationRestController {

    private final CreateImmoUseCase createImmoUseCase;
    private final UpdateImmoUseCase updateImmoUseCase;
    private final GetImmoByIdUseCase getImmoByIdUseCase;
    private final GetPageImmobilisationsUseCase getPageImmobilisationsUseCase;
    private final GetPlanAmortissementUseCase getPlanAmortissementUseCase;
    private final GenerateAmortisationUseCase generateAmortisationUseCase;
    private final ImmoValidateOrRejectUseCase immoValidateOrRejectUseCase;
    private final GetPendingImmoRequestsUseCase getPendingImmoRequestsUseCase;

    /**
     * Demandes en attente de décision — la boîte de réception du checker.
     *
     * <p>
     * L'{@code id} de chaque ligne est celui à passer à
     * {@code validate_or_reject} ; {@code entityPk} dit sur quel bien elle
     * porte. Exige {@code VALIDATE_OR_REJECT_IMMOBILISATION}.
     * </p>
     */
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<MakerCheckerSmartOutput> getPending(@PageableDefault Pageable pageable) {
        return getPendingImmoRequestsUseCase.execute(pageable);
    }

    /**
     * Initialise la création d'une immobilisation.
     *
     * <p>
     * Répond {@code 202} et non {@code 201} : rien n'est créé tant qu'un
     * checker n'a pas accepté. Le corps porte les deux identifiants dont le
     * client a besoin ensuite — celui de la demande, celui du bien à venir.
     * </p>
     */
    @PostMapping("/init_create")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<SubmissionOutput> initCreateImmo(@Valid @RequestBody CreateImmoRequest request) {
        CreateImmoInput input = new CreateImmoInput(
                request.code(),
                request.designation(),
                request.valeurOrigine(),
                request.dateAcquisition(),
                request.dateMiseEnService(),
                request.modeAmortissement(),
                request.dureeUtile(),
                request.valeurResiduelle()
        );
        return ResponseEntity.accepted().body(createImmoUseCase.execute(input));
    }

    /**
     * Initialise la modification d'une immobilisation.
     *
     * <p>
     * Comme la création, la demande passe par le circuit maker-checker : la
     * fiche ne bouge qu'une fois la décision rendue.
     * </p>
     */
    @PutMapping("/init_update/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<SubmissionOutput> initUpdateImmo(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateImmoRequest request
    ) {
        UpdateImmoInput input = new UpdateImmoInput(
                id,
                request.code(),
                request.designation(),
                request.valeurOrigine(),
                request.dateAcquisition(),
                request.dateMiseEnService(),
                request.modeAmortissement(),
                request.dureeUtile(),
                request.valeurResiduelle(),
                request.statut()
        );
        return ResponseEntity.accepted().body(updateImmoUseCase.execute(input));
    }

    /**
     * Registre des immobilisations.
     *
     * @param recherche fragment cherché dans le code ou la désignation, facultatif
     * @param statut restriction sur l'état du bien, facultative
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageRecord<ImmoOutput> getImmobilisations(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) StatutImmobilisation statut
    ) {
        return getPageImmobilisationsUseCase.execute(new RechercheImmoInput(pageable, recherche, statut));
    }

    /**
     * Récupère une immobilisation par son identifiant.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ImmoOutput getImmoById(@PathVariable UUID id) {
        return getImmoByIdUseCase.execute(id);
    }

    /**
     * Récupère le plan d'amortissement prévisionnel d'une immobilisation.
     */
    @GetMapping("/{id}/plan")
    @ResponseStatus(HttpStatus.OK)
    public List<AmortissementLineOutput> getPlanAmortissement(@PathVariable UUID id) {
        return getPlanAmortissementUseCase.execute(id);
    }

    /**
     * Initialise la génération des amortissements de fin d'exercice.
     */
    @PostMapping("/init_amortir")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<SubmissionOutput> initAmortir(@Valid @RequestBody GenerateAmortisationRequest request) {
        GenerateAmortisationInput input = new GenerateAmortisationInput(
                request.annee(),
                request.compteImmoCode()
        );
        return ResponseEntity.accepted().body(generateAmortisationUseCase.execute(input));
    }

    /**
     * Valide ou rejette une demande (Maker-Checker).
     */
    @PutMapping("/validate_or_reject/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void validateOrRejectImmo(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateImmoSubmitRequest request
    ) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        immoValidateOrRejectUseCase.execute(input);
    }
}
