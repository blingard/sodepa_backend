package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.GetGrandLivreInput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetGrandLivreUseCase implements UseCase<GetGrandLivreInput, List<ReportingUseCase.GrandLivreAccount>> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public List<ReportingUseCase.GrandLivreAccount> execute(GetGrandLivreInput input) {
        return reportingUseCase.genererGrandLivre(input.debut(), input.fin());
    }
}
