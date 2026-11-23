package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.source.application.inputs.ListerSourcesInput;
import com.sodepa.erp.budgetaire.source.application.outputs.SourceFinancementOutput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Liste des sources de financement. */
@Service
@RequiredArgsConstructor
public class ListerSourcesUseCase implements UseCase<ListerSourcesInput, PageRecord<SourceFinancementOutput>> {

    private final SourceFinancementAdapter adapter;

    @Override
    public PageRecord<SourceFinancementOutput> execute(ListerSourcesInput input) {
        return adapter.lister(input);
    }
}
