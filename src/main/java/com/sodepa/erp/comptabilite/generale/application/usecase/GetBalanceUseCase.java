package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.GetBalanceInput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBalanceUseCase implements UseCase<GetBalanceInput, List<ReportingUseCase.BalanceLine>> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public List<ReportingUseCase.BalanceLine> execute(GetBalanceInput input) {
        return reportingUseCase.genererBalance(input.debut(), input.fin());
    }
}
