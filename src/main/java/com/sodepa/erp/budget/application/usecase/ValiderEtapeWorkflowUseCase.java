package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.ValiderEtapeInput;
import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValiderEtapeWorkflowUseCase implements UseCase<ValiderEtapeInput, Void> {
    private final RapprochementAdapter adapter;

    @Override
    public Void execute(ValiderEtapeInput input) {
        adapter.validerEtapeWorkflow(input);
        return null;
    }
}
