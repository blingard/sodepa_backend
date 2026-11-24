package com.sodepa.erp.budgetaire.exercice.application.usecase;

import com.sodepa.erp.budgetaire.exercice.application.inputs.CreerExerciceInput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.adapter.ExerciceBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la création d'un exercice budgétaire au maker-checker (US-BUD-101). */
@Service
@RequiredArgsConstructor
public class CreerExerciceUseCase implements UseCase<CreerExerciceInput, SubmissionOutput> {

    private final ExerciceBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(CreerExerciceInput input) {
        return adapter.initCreer(input);
    }
}
