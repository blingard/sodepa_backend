package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UseCase pour consulter un relevé bancaire et ses lignes. */
@Service
@RequiredArgsConstructor
public class GetReleveByIdUseCase implements UseCase<UUID, ReleveBancaireOutput> {
    private final RapprochementAdapter rapprochementAdapter;

    @Override
    public ReleveBancaireOutput execute(UUID input) {
        return rapprochementAdapter.getReleveById(input);
    }
}
