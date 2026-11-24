package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.structure.application.inputs.ListerStructuresInput;
import com.sodepa.erp.budgetaire.structure.application.outputs.StructureBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Liste des structures budgétaires. */
@Service
@RequiredArgsConstructor
public class ListerStructuresUseCase implements UseCase<ListerStructuresInput, PageRecord<StructureBudgetaireOutput>> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public PageRecord<StructureBudgetaireOutput> execute(ListerStructuresInput input) {
        return adapter.lister(input);
    }
}
