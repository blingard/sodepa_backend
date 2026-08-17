package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TiersValidateOrRejectUseCase implements UseCase<ValidateOrRejectSubmissionInput, Void> {
    private final TiersAdapter tiersAdapter;

    @Override
    public Void execute(ValidateOrRejectSubmissionInput input) {
        log.info("Validation ou rejet pour le tiers de demande ID: {}", input.id());
        tiersAdapter.validateOrReject(input);
        return null;
    }
}
