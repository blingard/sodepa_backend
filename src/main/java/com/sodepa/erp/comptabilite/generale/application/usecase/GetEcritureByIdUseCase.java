package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.EcritureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * UseCase pour récupérer une écriture par son identifiant.
 */
@Service
@RequiredArgsConstructor
public class GetEcritureByIdUseCase implements UseCase<UUID, EcritureOutput> {

    private final EcritureAdapter adapter;

    @Override
    public EcritureOutput execute(UUID input) {
        return adapter.getEcritureById(input);
    }
}
