package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.RejeterInput;
import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RejeterEngagementUseCase implements UseCase<RejeterInput, Void> {
    private final RapprochementAdapter adapter;

    @Override
    public Void execute(RejeterInput input) {
        adapter.rejeterEngagement(input);
        return null;
    }
}
