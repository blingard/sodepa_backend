package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `GetTftUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class GetTftUseCase implements UseCase<Integer, ReportingUseCase.TftReport> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public ReportingUseCase.TftReport execute(Integer annee) {
        return reportingUseCase.genererTft(annee);
    }
}
