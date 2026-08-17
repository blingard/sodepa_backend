package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour valider ou rejeter une opération sur un compte.
 */
@Service
@RequiredArgsConstructor
public class CompteValidateOrRejectUseCase implements UseCase<ValidateOrRejectSubmissionInput, Void> {
    private final CompteAdapter compteAdapter;

    @Override
    public Void execute(ValidateOrRejectSubmissionInput input) {
        compteAdapter.validateOrReject(input);
        return null;
    }
}
