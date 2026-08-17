package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.CompteSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * UseCase pour récupérer un compte actif par son ID.
 */
@Service
@RequiredArgsConstructor
public class GetActiveCompteByIdUseCase implements UseCase<UUID, CompteSmartOutput> {
    private final CompteAdapter compteAdapter;

    @Override
    public CompteSmartOutput execute(UUID input) {
        return compteAdapter.getActiveCompteById(input);
    }
}
