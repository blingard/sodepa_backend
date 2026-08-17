package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CouvertureInput;
import com.sodepa.erp.budget.application.outputs.ContratCouvertureOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnregistrerCouvertureUseCase implements UseCase<CouvertureInput, ContratCouvertureOutput> {
    private final TresorerieAdapter adapter;

    @Override
    public ContratCouvertureOutput execute(CouvertureInput input) {
        return adapter.enregistrerCouverture(input);
    }
}
