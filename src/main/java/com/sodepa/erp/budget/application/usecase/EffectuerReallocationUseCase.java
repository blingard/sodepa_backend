package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.ReallocationInput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EffectuerReallocationUseCase implements UseCase<ReallocationInput, Void> {
    private final BudgetPlanAdapter adapter;

    @Override
    public Void execute(ReallocationInput input) {
        adapter.effectuerReallocation(input);
        return null;
    }
}
