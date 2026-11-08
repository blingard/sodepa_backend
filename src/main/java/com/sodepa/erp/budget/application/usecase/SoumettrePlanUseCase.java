package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `SoumettrePlanUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class SoumettrePlanUseCase implements UseCase<SoumettrePlanUseCase.Input, Void> {
    public record Input(UUID planId, UUID userId) {}
    private final BudgetPlanAdapter adapter;

    @Override
    public Void execute(Input input) {
        adapter.soumettrePlan(input.planId(), input.userId());
        return null;
    }
}
