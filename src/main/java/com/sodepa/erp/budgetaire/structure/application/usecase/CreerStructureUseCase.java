package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.structure.application.inputs.CreerStructureInput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la déclaration d'une unité comme structure budgétaire. */
@Service
@RequiredArgsConstructor
public class CreerStructureUseCase implements UseCase<CreerStructureInput, SubmissionOutput> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(CreerStructureInput input) {
        return adapter.initCreer(input);
    }
}
