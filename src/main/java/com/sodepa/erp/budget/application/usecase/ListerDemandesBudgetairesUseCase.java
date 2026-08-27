package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.RechercheDemandeInput;
import com.sodepa.erp.budget.application.outputs.BudgetDemandeOutput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetCollaboratifAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** UseCase pour lister les demandes budgétaires départementales. */
@Service
@RequiredArgsConstructor
public class ListerDemandesBudgetairesUseCase implements UseCase<RechercheDemandeInput, List<BudgetDemandeOutput>> {
    private final BudgetCollaboratifAdapter adapter;

    @Override
    public List<BudgetDemandeOutput> execute(RechercheDemandeInput input) {
        return adapter.listerDemandes(input);
    }
}
