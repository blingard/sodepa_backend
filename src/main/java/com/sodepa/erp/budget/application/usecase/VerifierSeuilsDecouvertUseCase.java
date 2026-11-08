package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.OverdraftAlertOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `VerifierSeuilsDecouvertUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class VerifierSeuilsDecouvertUseCase implements UseCase<Void, List<OverdraftAlertOutput>> {
    private final TresorerieAdapter adapter;

    @Override
    public List<OverdraftAlertOutput> execute(Void input) {
        return adapter.verifierSeuilsDecouvert();
    }
}
