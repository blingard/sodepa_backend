package com.sodepa.erp.budgetaire.exercice.application.usecase;

import com.sodepa.erp.budgetaire.exercice.application.outputs.ExerciceBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.adapter.ExerciceBudgetaireAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Liste les exercices budgétaires, du plus récent au plus ancien. */
@Service
@RequiredArgsConstructor
public class ListerExercicesUseCase implements UseCase<Pageable, PageRecord<ExerciceBudgetaireOutput>> {

    private final ExerciceBudgetaireAdapter adapter;

    @Override
    public PageRecord<ExerciceBudgetaireOutput> execute(Pageable pageable) {
        return adapter.lister(pageable);
    }
}
