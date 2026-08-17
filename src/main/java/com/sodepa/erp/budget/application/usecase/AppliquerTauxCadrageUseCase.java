package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CadrageInput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppliquerTauxCadrageUseCase implements UseCase<CadrageInput, Void> {
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public Void execute(CadrageInput input) {
        adapter.appliquerTauxCadrage(input);
        return null;
    }
}
