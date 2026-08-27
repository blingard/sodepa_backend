package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.RechercheEngagementInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** UseCase pour parcourir les engagements budgétaires. */
@Service
@RequiredArgsConstructor
public class GetPageEngagementsUseCase implements UseCase<RechercheEngagementInput, PageRecord<BudgetEngagementOutput>> {
    private final BudgetPlanAdapter adapter;

    @Override
    public PageRecord<BudgetEngagementOutput> execute(RechercheEngagementInput input) {
        return adapter.getEngagementsByPage(input);
    }
}
