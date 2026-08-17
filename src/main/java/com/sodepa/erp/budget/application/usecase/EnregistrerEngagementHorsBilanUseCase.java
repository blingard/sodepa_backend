package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CreerHorsBilanInput;
import com.sodepa.erp.budget.application.outputs.EngagementHorsBilanOutput;
import com.sodepa.erp.budget.infrastructure.adapter.ReportingFinancierAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnregistrerEngagementHorsBilanUseCase implements UseCase<CreerHorsBilanInput, EngagementHorsBilanOutput> {

    private final ReportingFinancierAdapter reportingFinancierAdapter;

    @Override
    public EngagementHorsBilanOutput execute(CreerHorsBilanInput input) {
        return reportingFinancierAdapter.enregistrerEngagementHorsBilan(input);
    }
}
