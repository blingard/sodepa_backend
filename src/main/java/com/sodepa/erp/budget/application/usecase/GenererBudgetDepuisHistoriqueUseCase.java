package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.GenererHistoriqueInput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenererBudgetDepuisHistoriqueUseCase implements UseCase<GenererHistoriqueInput, Void> {
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public Void execute(GenererHistoriqueInput input) {
        adapter.genererBudgetDepuisHistorique(input);
        return null;
    }
}
