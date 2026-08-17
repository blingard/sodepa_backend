package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.EngagementInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnregistrerEngagementUseCase implements UseCase<EngagementInput, BudgetEngagementOutput> {
    private final BudgetPlanAdapter adapter;

    @Override
    public BudgetEngagementOutput execute(EngagementInput input) {
        return adapter.enregistrerEngagement(input);
    }
}
