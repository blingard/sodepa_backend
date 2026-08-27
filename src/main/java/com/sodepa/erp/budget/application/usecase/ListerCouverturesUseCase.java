package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.RechercheCouvertureInput;
import com.sodepa.erp.budget.application.outputs.ContratCouvertureOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** UseCase pour lister le portefeuille de couvertures de change. */
@Service
@RequiredArgsConstructor
public class ListerCouverturesUseCase implements UseCase<RechercheCouvertureInput, List<ContratCouvertureOutput>> {
    private final TresorerieAdapter adapter;

    @Override
    public List<ContratCouvertureOutput> execute(RechercheCouvertureInput input) {
        return adapter.listerCouvertures(input);
    }
}
