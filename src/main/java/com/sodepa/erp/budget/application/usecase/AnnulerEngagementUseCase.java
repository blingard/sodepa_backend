package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnulerEngagementUseCase implements UseCase<AnnulerEngagementUseCase.Input, Void> {
    public record Input(String numeroEngagement, UUID userId) {}
    private final BudgetPlanAdapter adapter;

    @Override
    public Void execute(Input input) {
        adapter.annulerEngagement(input.numeroEngagement(), input.userId());
        return null;
    }
}
