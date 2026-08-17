package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateCompteInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UseCase pour initier la mise à jour d'un compte.
 */
@Service
@RequiredArgsConstructor
public class UpdateCompteUseCase implements UseCase<UpdateCompteInput, Void> {
    private final CompteAdapter compteAdapter;

    @Override
    public Void execute(UpdateCompteInput input) {
        compteAdapter.updateCompte(input);
        return null;
    }
}
