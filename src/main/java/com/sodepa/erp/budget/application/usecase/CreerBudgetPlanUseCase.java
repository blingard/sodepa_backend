package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CreerBudgetPlanInput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreerBudgetPlanUseCase implements UseCase<CreerBudgetPlanInput, BudgetPlanOutput> {
    private final BudgetPlanAdapter adapter;

    @Override
    public BudgetPlanOutput execute(CreerBudgetPlanInput input) {
        return adapter.creerBudgetPlan(input);
    }
}
