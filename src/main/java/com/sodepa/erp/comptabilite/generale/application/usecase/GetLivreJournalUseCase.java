package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.GetLivreJournalInput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetLivreJournalUseCase implements UseCase<GetLivreJournalInput, List<ReportingUseCase.LivreJournalLine>> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public List<ReportingUseCase.LivreJournalLine> execute(GetLivreJournalInput input) {
        return reportingUseCase.genererLivreJournal(input.debut(), input.fin());
    }
}
