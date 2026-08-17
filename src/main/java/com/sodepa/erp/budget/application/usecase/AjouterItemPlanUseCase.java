package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.AjouterItemInput;
import com.sodepa.erp.budget.application.outputs.BudgetItemOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AjouterItemPlanUseCase implements UseCase<AjouterItemInput, BudgetItemOutput> {
    private final BudgetPlanAdapter adapter;

    @Override
    public BudgetItemOutput execute(AjouterItemInput input) {
        return adapter.ajouterItemAuPlan(input);
    }
}
