package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.source.application.inputs.ModifierSourceInput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet le changement de libellé ou de bailleur d'une source. */
@Service
@RequiredArgsConstructor
public class ModifierSourceUseCase implements UseCase<ModifierSourceInput, SubmissionOutput> {

    private final SourceFinancementAdapter adapter;

    @Override
    public SubmissionOutput execute(ModifierSourceInput input) {
        return adapter.initModifier(input);
    }
}
