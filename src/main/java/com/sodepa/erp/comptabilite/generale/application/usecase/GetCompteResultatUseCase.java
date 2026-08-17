package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCompteResultatUseCase implements UseCase<Integer, ReportingUseCase.CompteResultatReport> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public ReportingUseCase.CompteResultatReport execute(Integer annee) {
        return reportingUseCase.genererCompteResultat(annee);
    }
}
