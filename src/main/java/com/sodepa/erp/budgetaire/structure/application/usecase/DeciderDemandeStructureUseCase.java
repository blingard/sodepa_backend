package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.structure.application.inputs.DeciderDemandeStructureInput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Valide ou rejette une demande portant sur une structure budgétaire. */
@Service
@RequiredArgsConstructor
public class DeciderDemandeStructureUseCase implements UseCase<DeciderDemandeStructureInput, Void> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public Void execute(DeciderDemandeStructureInput input) {
        adapter.decider(input);
        return null;
    }
}
