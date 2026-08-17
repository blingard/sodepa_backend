package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.CashFlowMensuelOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenererCashFlowPrevisionnelUseCase implements UseCase<GenererCashFlowPrevisionnelUseCase.Input, List<CashFlowMensuelOutput>> {
    public record Input(LocalDate debut, LocalDate fin) {}
    private final TresorerieAdapter adapter;

    @Override
    public List<CashFlowMensuelOutput> execute(Input input) {
        return adapter.genererCashFlowPrevisionnel(input.debut(), input.fin());
    }
}
