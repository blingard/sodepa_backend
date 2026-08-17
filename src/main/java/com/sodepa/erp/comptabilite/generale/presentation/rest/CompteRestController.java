package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateCompteInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateCompteInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.CompteOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.CompteSmartOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import com.sodepa.erp.comptabilite.generale.presentation.requests.CreateCompteRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.RejectOrValidateCompteSubmitRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.UpdateCompteRequest;
import com.sodepa.erp.utils.PageRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API de gestion des comptes.
 */
@RestController
@RequestMapping("/api/v1/caccounting/compte")
@RequiredArgsConstructor
public class CompteRestController {
    private final CreateCompteUseCase createCompteUseCase;
    private final GetActiveCompteByIdUseCase getActiveCompteByIdUseCase;
    private final GetCompteByIdUseCase getCompteByIdUseCase;
    private final GetPageComptesUseCase getPageComptesUseCase;
    private final ListComptesUseCase listComptesUseCase;
    private final UpdateCompteUseCase updateCompteUseCase;
    private final DeleteCompteUseCase deleteCompteUseCase;
    private final CompteValidateOrRejectUseCase compteValidateOrRejectUseCase;

    @PostMapping("/init_create")
    public void createCompte(@Valid @RequestBody CreateCompteRequest request) {
        CreateCompteInput input = CreateCompteInput.builder()
                .code(request.code())
                .intitule(request.intitule())
                .parentCode(request.parentCode())
                .niveau(request.niveau())
                .typeAnalytique(request.typeAnalytique())
                .nature(request.nature())
                .isAuxiliaire(request.isAuxiliaire())
                .build();
        createCompteUseCase.execute(input);
    }

    @GetMapping("/active_by_id/{id}")
    public CompteSmartOutput getActiveCompteSmart(@Valid @PathVariable UUID id) {
        return getActiveCompteByIdUseCase.execute(id);
    }

    @GetMapping("/{id}")
    public CompteOutput getCompte(@Valid @PathVariable UUID id) {
        return getCompteByIdUseCase.execute(id);
    }

    @GetMapping
    public PageRecord<CompteSmartOutput> getComptePage(@Valid @PageableDefault Pageable pageable) {
        return getPageComptesUseCase.execute(pageable);
    }

    @GetMapping("/list")
    public List<CompteSmartOutput> getCompteList() {
        return listComptesUseCase.execute(null);
    }

    @PutMapping("/init_update/{id}")
    public void updateCompte(@Valid @RequestBody UpdateCompteRequest request, @Valid @PathVariable UUID id) {
        UpdateCompteInput input = UpdateCompteInput.builder()
                .id(id)
                .code(request.code())
                .intitule(request.intitule())
                .parentCode(request.parentCode())
                .niveau(request.niveau())
                .typeAnalytique(request.typeAnalytique())
                .nature(request.nature())
                .isAuxiliaire(request.isAuxiliaire())
                .build();
        updateCompteUseCase.execute(input);
    }

    @DeleteMapping("/{id}")
    public void deleteCompte(@Valid @PathVariable UUID id) {
        deleteCompteUseCase.execute(id);
    }

    @PutMapping("/validate_or_reject/{id}")
    public void validateOrRejectUpdate(@NotNull @PathVariable UUID id, @Valid @RequestBody RejectOrValidateCompteSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        compteValidateOrRejectUseCase.execute(input);
    }
}
