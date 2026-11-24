package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.structure.application.inputs.ChangerEtatStructureInput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la désactivation d'une structure budgétaire. */
@Service
@RequiredArgsConstructor
public class DesactiverStructureUseCase implements UseCase<ChangerEtatStructureInput, SubmissionOutput> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(ChangerEtatStructureInput input) {
        return adapter.initDesactiver(input);
    }
}
