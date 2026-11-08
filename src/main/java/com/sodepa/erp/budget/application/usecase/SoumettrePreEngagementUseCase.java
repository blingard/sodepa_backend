package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.PreEngagementInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `SoumettrePreEngagementUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class SoumettrePreEngagementUseCase implements UseCase<PreEngagementInput, BudgetEngagementOutput> {
    private final RapprochementAdapter adapter;

    @Override
    public BudgetEngagementOutput execute(PreEngagementInput input) {
        return adapter.soumettrePreEngagement(input);
    }
}
