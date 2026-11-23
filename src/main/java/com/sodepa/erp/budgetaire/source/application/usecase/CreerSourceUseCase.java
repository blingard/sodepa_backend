package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.source.application.inputs.CreerSourceInput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la création d'une source de financement. */
@Service
@RequiredArgsConstructor
public class CreerSourceUseCase implements UseCase<CreerSourceInput, SubmissionOutput> {

    private final SourceFinancementAdapter adapter;

    @Override
    public SubmissionOutput execute(CreerSourceInput input) {
        return adapter.initCreer(input);
    }
}
