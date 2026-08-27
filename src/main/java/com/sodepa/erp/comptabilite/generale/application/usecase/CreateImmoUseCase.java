package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateImmoInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour initier la création d'une immobilisation.
 */
@Service
@RequiredArgsConstructor
public class CreateImmoUseCase implements UseCase<CreateImmoInput, SubmissionOutput> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public SubmissionOutput execute(CreateImmoInput input) {
        return immobilisationAdapter.initCreateImmo(input);
    }
}
