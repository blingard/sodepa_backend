package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour valider ou rejeter une demande (Maker-Checker) concernant une immobilisation.
 */
@Service
@RequiredArgsConstructor
public class ImmoValidateOrRejectUseCase implements UseCase<ValidateOrRejectSubmissionInput, Void> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public Void execute(ValidateOrRejectSubmissionInput input) {
        immobilisationAdapter.validateOrReject(input);
        return null;
    }
}
