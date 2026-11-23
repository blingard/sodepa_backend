package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.nature.application.inputs.ModifierNatureInput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet le changement de libellé d'une nature. */
@Service
@RequiredArgsConstructor
public class ModifierNatureUseCase implements UseCase<ModifierNatureInput, SubmissionOutput> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(ModifierNatureInput input) {
        return adapter.initModifier(input);
    }
}
