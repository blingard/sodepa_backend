package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.nature.application.inputs.CreerNatureInput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la création d'une nature budgétaire. */
@Service
@RequiredArgsConstructor
public class CreerNatureUseCase implements UseCase<CreerNatureInput, SubmissionOutput> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(CreerNatureInput input) {
        return adapter.initCreer(input);
    }
}
