package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.structure.application.inputs.ModifierStructureInput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet le changement d'ordonnateur ou de service d'engagement d'une structure. */
@Service
@RequiredArgsConstructor
public class ModifierStructureUseCase implements UseCase<ModifierStructureInput, SubmissionOutput> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(ModifierStructureInput input) {
        return adapter.initModifier(input);
    }
}
