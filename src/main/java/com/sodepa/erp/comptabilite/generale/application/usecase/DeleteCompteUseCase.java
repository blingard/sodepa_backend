package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * UseCase pour supprimer un compte.
 */
@Service
@RequiredArgsConstructor
public class DeleteCompteUseCase implements UseCase<UUID, Void> {
    private final CompteAdapter compteAdapter;

    @Override
    public Void execute(UUID input) {
        compteAdapter.deleteCompte(input);
        return null;
    }
}
