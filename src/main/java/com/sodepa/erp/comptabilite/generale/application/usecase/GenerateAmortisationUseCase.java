package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.GenerateAmortisationInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour initier la génération des écritures d'amortissement en fin d'exercice.
 */
@Service
@RequiredArgsConstructor
public class GenerateAmortisationUseCase implements UseCase<GenerateAmortisationInput, SubmissionOutput> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public SubmissionOutput execute(GenerateAmortisationInput input) {
        return immobilisationAdapter.initGenerateAmortisation(input);
    }
}
