package com.sodepa.erp.budgetaire.exercice.application.usecase;

import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.application.outputs.ExerciceBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.adapter.ExerciceBudgetaireAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Demandes sur les exercices en attente de décision, hors celles de l'appelant.
 *
 * <p>C'est la boîte de réception du validateur : sans elle, l'identifiant de
 * demande rendu à la soumission était le seul chemin vers sa décision.</p>
 */
@Service
@RequiredArgsConstructor
public class ListerDemandesExerciceEnAttenteUseCase
        implements UseCase<Pageable, PageRecord<DemandeBudgetaireOutput<ExerciceBudgetaireOutput>>> {

    private final ExerciceBudgetaireAdapter adapter;

    @Override
    public PageRecord<DemandeBudgetaireOutput<ExerciceBudgetaireOutput>> execute(Pageable pageable) {
        return adapter.demandesEnAttente(pageable);
    }
}
