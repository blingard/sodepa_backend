package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.application.outputs.NatureBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Demandes sur les natures en attente de décision, hors celles de l'appelant. */
@Service
@RequiredArgsConstructor
public class ListerDemandesNatureEnAttenteUseCase implements UseCase<Pageable, PageRecord<DemandeBudgetaireOutput<NatureBudgetaireOutput>>> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public PageRecord<DemandeBudgetaireOutput<NatureBudgetaireOutput>> execute(Pageable input) {
        return adapter.demandesEnAttente(input);
    }
}
