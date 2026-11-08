package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `RejeterPlanUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class RejeterPlanUseCase implements UseCase<RejeterPlanUseCase.Input, Void> {
    public record Input(UUID planId, UUID userId) {}
    private final BudgetPlanAdapter adapter;

    @Override
    public Void execute(Input input) {
        adapter.rejeterPlan(input.planId(), input.userId());
        return null;
    }
}
