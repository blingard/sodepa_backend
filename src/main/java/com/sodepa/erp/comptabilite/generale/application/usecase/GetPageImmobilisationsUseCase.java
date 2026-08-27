package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.RechercheImmoInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ImmoOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour parcourir le registre des immobilisations.
 */
@Service
@RequiredArgsConstructor
public class GetPageImmobilisationsUseCase implements UseCase<RechercheImmoInput, PageRecord<ImmoOutput>> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public PageRecord<ImmoOutput> execute(RechercheImmoInput input) {
        return immobilisationAdapter.getImmobilisationsByPage(input);
    }
}
