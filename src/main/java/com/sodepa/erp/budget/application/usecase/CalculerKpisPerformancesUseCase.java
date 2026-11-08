package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.KpiReportOutput;
import com.sodepa.erp.budget.infrastructure.adapter.ReportingFinancierAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `CalculerKpisPerformancesUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class CalculerKpisPerformancesUseCase implements UseCase<Void, KpiReportOutput> {

    private final ReportingFinancierAdapter reportingFinancierAdapter;

    @Override
    public KpiReportOutput execute(Void input) {
        return reportingFinancierAdapter.genererRapportKpis();
    }
}
