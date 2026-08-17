package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsoliderDemandesDansPlanUseCase implements UseCase<ConsoliderDemandesDansPlanUseCase.Input, Void> {
    public record Input(int annee, UUID planId, UUID userId) {}
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public Void execute(Input input) {
        adapter.consoliderDemandesDansPlan(input.annee(), input.planId(), input.userId());
        return null;
    }
}
