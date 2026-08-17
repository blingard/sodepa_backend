package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.SaisieEcritureInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.EcritureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase pour la saisie d'une écriture comptable.
 */
@Service
@RequiredArgsConstructor
public class SaisirEcritureUseCase implements UseCase<SaisieEcritureInput, EcritureOutput> {

    private final EcritureAdapter adapter;

    @Override
    @Transactional
    public EcritureOutput execute(SaisieEcritureInput input) {
        return adapter.saisirEcriture(input);
    }
}
