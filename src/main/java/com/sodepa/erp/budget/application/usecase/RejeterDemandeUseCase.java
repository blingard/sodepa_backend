package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejeterDemandeUseCase implements UseCase<RejeterDemandeUseCase.Input, Void> {
    public record Input(UUID demandeId, String motif, UUID valideurId) {}
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public Void execute(Input input) {
        adapter.rejeterDemande(input.demandeId(), input.motif(), input.valideurId());
        return null;
    }
}
