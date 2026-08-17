package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.GenerateAmortisationInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.AmortissementLineOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ImmoOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.CreateImmoUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GenerateAmortisationUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetImmoByIdUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetPlanAmortissementUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.ImmoValidateOrRejectUseCase;
import com.sodepa.erp.comptabilite.generale.presentation.requests.CreateImmoRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.GenerateAmortisationRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.RejectOrValidateImmoSubmitRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final GetImmoByIdUseCase getImmoByIdUseCase;
    private final GetPlanAmortissementUseCase getPlanAmortissementUseCase;
    private final GenerateAmortisationUseCase generateAmortisationUseCase;
    private final ImmoValidateOrRejectUseCase immoValidateOrRejectUseCase;

    /**
     * Initialise la création d'une immobilisation.
     */
    @PostMapping("/init_create")
    @ResponseStatus(HttpStatus.CREATED)
    public void initCreateImmo(@Valid @RequestBody CreateImmoRequest request) {
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
        createImmoUseCase.execute(input);
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
    @ResponseStatus(HttpStatus.OK)
    public void initAmortir(@Valid @RequestBody GenerateAmortisationRequest request) {
        GenerateAmortisationInput input = new GenerateAmortisationInput(
                request.annee(),
                request.compteImmoCode()
        );
        generateAmortisationUseCase.execute(input);
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
