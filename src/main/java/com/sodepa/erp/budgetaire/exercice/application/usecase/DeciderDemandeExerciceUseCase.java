package com.sodepa.erp.budgetaire.exercice.application.usecase;

import com.sodepa.erp.budgetaire.exercice.application.inputs.DeciderDemandeExerciceInput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.adapter.ExerciceBudgetaireAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Valide ou rejette une demande portant sur un exercice budgétaire. */
@Service
@RequiredArgsConstructor
public class DeciderDemandeExerciceUseCase implements UseCase<DeciderDemandeExerciceInput, Void> {

    private final ExerciceBudgetaireAdapter adapter;

    @Override
    public Void execute(DeciderDemandeExerciceInput input) {
        adapter.decider(input);
        return null;
    }
}
