package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.RechercheBudgetPlanInput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanSmartOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** UseCase pour parcourir les plans budgétaires. */
@Service
@RequiredArgsConstructor
public class GetPageBudgetPlansUseCase implements UseCase<RechercheBudgetPlanInput, PageRecord<BudgetPlanSmartOutput>> {
    private final BudgetPlanAdapter adapter;

    @Override
    public PageRecord<BudgetPlanSmartOutput> execute(RechercheBudgetPlanInput input) {
        return adapter.getPlansByPage(input);
    }
}
