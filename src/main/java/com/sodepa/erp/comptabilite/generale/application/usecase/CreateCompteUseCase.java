package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateCompteInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour initier la création d'un compte.
 */
@Service
@RequiredArgsConstructor
public class CreateCompteUseCase implements UseCase<CreateCompteInput, Void> {
    private final CompteAdapter compteAdapter;

    @Override
    public Void execute(CreateCompteInput input) {
        compteAdapter.initCreateCompte(input);
        return null;
    }
}
