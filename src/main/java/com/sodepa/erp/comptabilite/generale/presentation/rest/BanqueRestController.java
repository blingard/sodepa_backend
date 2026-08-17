package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateBankImageInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateBankInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.BankOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.BankSmartOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import com.sodepa.erp.comptabilite.generale.presentation.requests.CreateBankRequest;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateBankInput;
import com.sodepa.erp.comptabilite.generale.presentation.requests.RejectOrValidateBankSubmitRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.UpdateBankRequest;
import com.sodepa.erp.utils.PageRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.Set;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API de gestion des banques partenaires.
 */
@RestController
@RequestMapping("/api/v1/caccounting/bank")
@RequiredArgsConstructor
public class BanqueRestController {
    private final CreateBankUseCase createBankUseCase;
    private final GetActiveBankByIdUseCase getActiveBankByIdUseCase;
    private final GetBankByIdUseCase getBankByIdUseCase;
    private final GetPageBankInfosUseCase getPageBankInfosUseCase;
    private final ListBankSmartInfosUseCase listBankSmartInfosUseCase;
    private final UpdateBankImageUseCase updateBankImageUseCase;
    private final UpdateBankUseCase updateBankUseCase;
    private final BankValidateOrRejectUseCase bankValidateOrRejectUseCase;


    @PostMapping("init_create")
    public void createBank(@Valid @RequestBody CreateBankRequest request, @RequestPart(name = "file")MultipartFile logo) {
        CreateBankInput input = new CreateBankInput(request.code(), request.name(), request.accountAccountingCode(), logo);
        createBankUseCase.execute(input);
    }

    @GetMapping("active_by_id/{id}")
    public BankSmartOutput getActiveBankSmart(@Valid @PathVariable UUID id) {
        return getActiveBankByIdUseCase.execute(id);
    }

    @GetMapping("{id}")
    public BankOutput getBankSmart(@Valid @PathVariable UUID id) {
        return getBankByIdUseCase.execute(id);
    }

    @GetMapping
    public PageRecord<BankSmartOutput> getBankPage(@Valid @PageableDefault Pageable pageable) {
        return getPageBankInfosUseCase.execute(pageable);
    }

    @GetMapping("list")
    public Set<BankSmartOutput> getBankPage() {
        return listBankSmartInfosUseCase.execute(null);
    }

    @PutMapping("init_update/{id}")
    public void update(@Valid @RequestBody UpdateBankRequest request, @Valid @PathVariable UUID id) {
        UpdateBankInput input = new UpdateBankInput(id, request.code(), request.name(), request.accountingCode(),
                request.logo(), request.status());
        updateBankUseCase.execute(input);
    }

    @PutMapping("init_update_image/{id}")
    public void updateLogo(@NotNull @RequestPart(name = "file") MultipartFile logo, @Valid @PathVariable UUID id) {
        UpdateBankImageInput input = new UpdateBankImageInput(id, logo);
        updateBankImageUseCase.execute(input);
    }

    @PutMapping("validate_or_reject/{id}")
    public void validateOrRejectUpdate(@NotNull @PathVariable UUID id, @Valid @RequestBody RejectOrValidateBankSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(id,
                request.decision(), request.notes(), request.checkerOperationType());
        bankValidateOrRejectUseCase.execute(input);
    }
}
