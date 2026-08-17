package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateTiersInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateTiersInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.TiersOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.TiersSmartOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import com.sodepa.erp.comptabilite.generale.presentation.requests.CreateTiersRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.UpdateTiersRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.RejectOrValidateTiersSubmitRequest;
import com.sodepa.erp.utils.PageRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/caccounting/tiers")
@RequiredArgsConstructor
public class TiersRestController {

    private final CreateTiersUseCase createTiersUseCase;
    private final GetActiveTiersByIdUseCase getActiveTiersByIdUseCase;
    private final GetTiersByIdUseCase getTiersByIdUseCase;
    private final GetPageTiersUseCase getPageTiersUseCase;
    private final ListTiersUseCase listTiersUseCase;
    private final UpdateTiersUseCase updateTiersUseCase;
    private final TiersValidateOrRejectUseCase tiersValidateOrRejectUseCase;

    @PostMapping("/init_create")
    public void createTiers(@Valid @RequestBody CreateTiersRequest request) {
        CreateTiersInput input = new CreateTiersInput(
                request.code(),
                request.raisonSociale(),
                request.adresse(),
                request.telephone(),
                request.email(),
                request.typeTiers(),
                request.compteCollectifCode()
        );
        createTiersUseCase.execute(input);
    }

    @GetMapping("/active_by_id/{id}")
    public TiersSmartOutput getActiveTiersById(@PathVariable UUID id) {
        return getActiveTiersByIdUseCase.execute(id);
    }

    @GetMapping("/{id}")
    public TiersOutput getTiersById(@PathVariable UUID id) {
        return getTiersByIdUseCase.execute(id);
    }

    @GetMapping
    public PageRecord<TiersSmartOutput> getTiersByPage(Pageable pageable) {
        return getPageTiersUseCase.execute(pageable);
    }

    @GetMapping("/list")
    public Set<TiersSmartOutput> listAllActiveTiers() {
        return listTiersUseCase.execute(null);
    }

    @PutMapping("/init_update/{id}")
    public void updateTiers(@PathVariable UUID id, @Valid @RequestBody UpdateTiersRequest request) {
        UpdateTiersInput input = new UpdateTiersInput(
                id,
                request.code(),
                request.raisonSociale(),
                request.adresse(),
                request.telephone(),
                request.email(),
                request.typeTiers(),
                request.compteCollectifCode(),
                request.actif()
        );
        updateTiersUseCase.execute(input);
    }

    @PutMapping("/validate_or_reject/{id}")
    public void validateOrReject(@PathVariable UUID id, @Valid @RequestBody RejectOrValidateTiersSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        tiersValidateOrRejectUseCase.execute(input);
    }
}
