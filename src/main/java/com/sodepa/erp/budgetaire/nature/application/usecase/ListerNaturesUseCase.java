package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.nature.application.inputs.ListerNaturesInput;
import com.sodepa.erp.budgetaire.nature.application.outputs.NatureBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Liste des natures budgétaires. */
@Service
@RequiredArgsConstructor
public class ListerNaturesUseCase implements UseCase<ListerNaturesInput, PageRecord<NatureBudgetaireOutput>> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public PageRecord<NatureBudgetaireOutput> execute(ListerNaturesInput input) {
        return adapter.lister(input);
    }
}
