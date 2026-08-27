package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.RechercheFinancementInput;
import com.sodepa.erp.budget.application.outputs.FinancementSmartOutput;
import com.sodepa.erp.budget.infrastructure.adapter.FinancementAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** UseCase pour parcourir les financements. */
@Service
@RequiredArgsConstructor
public class GetPageFinancementsUseCase implements UseCase<RechercheFinancementInput, PageRecord<FinancementSmartOutput>> {
    private final FinancementAdapter adapter;

    @Override
    public PageRecord<FinancementSmartOutput> execute(RechercheFinancementInput input) {
        return adapter.getFinancementsByPage(input);
    }
}
