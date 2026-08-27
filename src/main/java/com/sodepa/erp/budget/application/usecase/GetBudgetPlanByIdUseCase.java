package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.BudgetPlanOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UseCase pour consulter un plan budgétaire et ses postes. */
@Service
@RequiredArgsConstructor
public class GetBudgetPlanByIdUseCase implements UseCase<UUID, BudgetPlanOutput> {
    private final BudgetPlanAdapter adapter;

    @Override
    public BudgetPlanOutput execute(UUID input) {
        return adapter.getPlanById(input);
    }
}
