package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.SimulationResultOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `SimulerHypothesesWhatIfUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class SimulerHypothesesWhatIfUseCase implements UseCase<SimulerHypothesesWhatIfUseCase.Input, SimulationResultOutput> {
    public record Input(BigDecimal croissance, BigDecimal inflation, BigDecimal prixRevient) {}
    private final TresorerieAdapter adapter;

    @Override
    public SimulationResultOutput execute(Input input) {
        return adapter.simulerHypotheses(input.croissance(), input.inflation(), input.prixRevient());
    }
}
