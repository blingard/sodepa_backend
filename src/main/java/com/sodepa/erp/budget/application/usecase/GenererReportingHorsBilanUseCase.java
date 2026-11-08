package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.EngagementHorsBilanOutput;
import com.sodepa.erp.budget.infrastructure.adapter.ReportingFinancierAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `GenererReportingHorsBilanUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class GenererReportingHorsBilanUseCase implements UseCase<Void, List<EngagementHorsBilanOutput>> {

    private final ReportingFinancierAdapter reportingFinancierAdapter;

    @Override
    public List<EngagementHorsBilanOutput> execute(Void input) {
        return reportingFinancierAdapter.genererReportingHorsBilan();
    }
}
