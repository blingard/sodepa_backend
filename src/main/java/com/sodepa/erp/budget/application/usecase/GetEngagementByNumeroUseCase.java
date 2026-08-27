package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** UseCase pour consulter un engagement par son numéro. */
@Service
@RequiredArgsConstructor
public class GetEngagementByNumeroUseCase implements UseCase<String, BudgetEngagementOutput> {
    private final BudgetPlanAdapter adapter;

    @Override
    public BudgetEngagementOutput execute(String input) {
        return adapter.getEngagementByNumero(input);
    }
}
