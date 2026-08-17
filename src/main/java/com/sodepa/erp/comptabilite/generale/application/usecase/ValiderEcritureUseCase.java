package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.EcritureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UseCase pour valider définitivement une écriture soumise.
 */
@Service
@RequiredArgsConstructor
public class ValiderEcritureUseCase implements UseCase<UUID, EcritureOutput> {

    private final EcritureAdapter adapter;

    @Override
    @Transactional
    public EcritureOutput execute(UUID input) {
        return adapter.validerEcriture(input);
    }
}
