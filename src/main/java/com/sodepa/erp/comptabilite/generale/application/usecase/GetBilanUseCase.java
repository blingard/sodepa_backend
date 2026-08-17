package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetBilanUseCase implements UseCase<LocalDate, ReportingUseCase.BilanReport> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public ReportingUseCase.BilanReport execute(LocalDate dateBilan) {
        return reportingUseCase.genererBilan(dateBilan);
    }
}
