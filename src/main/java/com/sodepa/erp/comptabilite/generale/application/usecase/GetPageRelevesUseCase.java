package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.RechercheReleveInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** UseCase pour parcourir les relevés bancaires importés. */
@Service
@RequiredArgsConstructor
public class GetPageRelevesUseCase implements UseCase<RechercheReleveInput, PageRecord<ReleveBancaireOutput>> {
    private final RapprochementAdapter rapprochementAdapter;

    @Override
    public PageRecord<ReleveBancaireOutput> execute(RechercheReleveInput input) {
        return rapprochementAdapter.getRelevesByPage(input);
    }
}
