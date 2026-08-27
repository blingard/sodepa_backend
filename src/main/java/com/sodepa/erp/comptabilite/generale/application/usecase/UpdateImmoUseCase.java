package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateImmoInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour initier la modification d'une immobilisation.
 */
@Service
@RequiredArgsConstructor
public class UpdateImmoUseCase implements UseCase<UpdateImmoInput, SubmissionOutput> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public SubmissionOutput execute(UpdateImmoInput input) {
        return immobilisationAdapter.initUpdateImmo(input);
    }
}
