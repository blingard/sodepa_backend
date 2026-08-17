package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cas d'utilisation pour valider ou rejeter une soumission de journal.
 */
@Component
@RequiredArgsConstructor
public class JournalValidateOrRejectUseCase implements UseCase<ValidateOrRejectSubmissionInput, Void> {

    private final JournalAdapter journalAdapter;

    @Override
    public Void execute(ValidateOrRejectSubmissionInput input) {
        journalAdapter.validateOrReject(input);
        return null;
    }
}
