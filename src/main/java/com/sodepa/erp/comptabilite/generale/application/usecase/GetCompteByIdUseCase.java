package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.CompteOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * UseCase pour récupérer un compte par son ID.
 */
@Service
@RequiredArgsConstructor
public class GetCompteByIdUseCase implements UseCase<UUID, CompteOutput> {
    private final CompteAdapter compteAdapter;

    @Override
    public CompteOutput execute(UUID input) {
        return compteAdapter.getCompteById(input);
    }
}
