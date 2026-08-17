package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CreerPrevisionInput;
import com.sodepa.erp.budget.application.outputs.PrevisionTresorerieOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AjouterPrevisionUseCase implements UseCase<CreerPrevisionInput, PrevisionTresorerieOutput> {
    private final TresorerieAdapter adapter;

    @Override
    public PrevisionTresorerieOutput execute(CreerPrevisionInput input) {
        return adapter.ajouterPrevision(input);
    }
}
