package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.SaisirDemandeInput;
import com.sodepa.erp.budget.application.outputs.BudgetDemandeOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaisirDemandeBudgetaireUseCase implements UseCase<SaisirDemandeInput, BudgetDemandeOutput> {
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public BudgetDemandeOutput execute(SaisirDemandeInput input) {
        return adapter.saisirDemande(input);
    }
}
