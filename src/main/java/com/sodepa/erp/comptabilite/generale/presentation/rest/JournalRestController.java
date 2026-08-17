package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateJournalInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateJournalInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalSmartOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import com.sodepa.erp.comptabilite.generale.presentation.requests.CreateJournalRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.RejectOrValidateJournalSubmitRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.UpdateJournalRequest;
import com.sodepa.erp.utils.PageRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API de gestion des journaux comptables.
 */
@RestController
@RequestMapping("/api/comptabilite/journaux")
@RequiredArgsConstructor
public class JournalRestController {

    private final CreateJournalUseCase createJournalUseCase;
    private final GetActiveJournalByIdUseCase getActiveJournalByIdUseCase;
    private final GetJournalByIdUseCase getJournalByIdUseCase;
    private final GetPageJournauxUseCase getPageJournauxUseCase;
    private final ListJournauxUseCase listJournauxUseCase;
    private final UpdateJournalUseCase updateJournalUseCase;
    private final ToggleActiveJournalUseCase toggleActiveJournalUseCase;
    private final JournalValidateOrRejectUseCase journalValidateOrRejectUseCase;

    @PostMapping("/init_create")
    public void initCreate(@Valid @RequestBody CreateJournalRequest request) {
        CreateJournalInput input = CreateJournalInput.builder()
                .code(request.code())
                .intitule(request.intitule())
                .typeJournal(request.typeJournal())
                .build();
        createJournalUseCase.execute(input);
    }

    @GetMapping("/active_by_id/{id}")
    public JournalSmartOutput getActiveById(@PathVariable UUID id) {
        return getActiveJournalByIdUseCase.execute(id);
    }

    @GetMapping("/{id}")
    public JournalOutput getById(@PathVariable UUID id) {
        return getJournalByIdUseCase.execute(id);
    }

    @GetMapping
    public PageRecord<JournalSmartOutput> getByPage(Pageable pageable) {
        return getPageJournauxUseCase.execute(pageable);
    }

    @GetMapping("/list")
    public List<JournalSmartOutput> listAll() {
        return listJournauxUseCase.execute(null);
    }

    @PutMapping("/init_update/{id}")
    public void initUpdate(@PathVariable UUID id, @Valid @RequestBody UpdateJournalRequest request) {
        UpdateJournalInput input = UpdateJournalInput.builder()
                .id(id)
                .code(request.code())
                .intitule(request.intitule())
                .typeJournal(request.typeJournal())
                .actif(request.actif())
                .build();
        updateJournalUseCase.execute(input);
    }

    @PutMapping("/{id}/toggle")
    public void toggleActive(@PathVariable UUID id) {
        toggleActiveJournalUseCase.execute(id);
    }

    @PutMapping("/validate_or_reject/{id}")
    public void validateOrReject(@PathVariable UUID id, @Valid @RequestBody RejectOrValidateJournalSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        journalValidateOrRejectUseCase.execute(input);
    }
}
