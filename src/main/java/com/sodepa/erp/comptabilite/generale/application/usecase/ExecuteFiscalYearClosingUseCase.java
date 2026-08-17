package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.FiscalYearClosingAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExecuteFiscalYearClosingUseCase implements UseCase<ValidateOrRejectSubmissionInput, Void> {

    private final FiscalYearClosingAdapter fiscalYearClosingAdapter;

    @Override
    public Void execute(ValidateOrRejectSubmissionInput input) {
        fiscalYearClosingAdapter.generateFiscalYearClosingSimulation(input);
        return null;
    }
}
