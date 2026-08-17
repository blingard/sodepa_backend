package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SoumettreDemandesUseCase implements UseCase<SoumettreDemandesUseCase.Input, Void> {
    public record Input(UUID departementId, int annee) {}
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public Void execute(Input input) {
        adapter.soumettreDemandes(input.departementId(), input.annee());
        return null;
    }
}
