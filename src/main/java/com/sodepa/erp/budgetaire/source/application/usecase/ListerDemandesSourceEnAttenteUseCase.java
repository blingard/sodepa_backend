package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.source.application.outputs.SourceFinancementOutput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Demandes sur les sources en attente de décision, hors celles de l'appelant. */
@Service
@RequiredArgsConstructor
public class ListerDemandesSourceEnAttenteUseCase implements UseCase<Pageable, PageRecord<DemandeBudgetaireOutput<SourceFinancementOutput>>> {

    private final SourceFinancementAdapter adapter;

    @Override
    public PageRecord<DemandeBudgetaireOutput<SourceFinancementOutput>> execute(Pageable input) {
        return adapter.demandesEnAttente(input);
    }
}
