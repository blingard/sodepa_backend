package com.sodepa.erp.budgetaire.exercice.application.usecase;

import com.sodepa.erp.budgetaire.exercice.application.outputs.ExerciceBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.adapter.ExerciceBudgetaireAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Lit un exercice budgétaire désigné par son année. */
@Service
@RequiredArgsConstructor
public class ConsulterExerciceUseCase implements UseCase<Integer, ExerciceBudgetaireOutput> {

    private final ExerciceBudgetaireAdapter adapter;

    @Override
    public ExerciceBudgetaireOutput execute(Integer annee) {
        return adapter.consulter(annee);
    }
}
