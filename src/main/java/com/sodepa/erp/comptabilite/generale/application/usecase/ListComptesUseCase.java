package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.CompteSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UseCase pour récupérer la liste complète de tous les comptes.
 */
@Service
@RequiredArgsConstructor
public class ListComptesUseCase implements UseCase<Void, List<CompteSmartOutput>> {
    private final CompteAdapter compteAdapter;

    @Override
    public List<CompteSmartOutput> execute(Void input) {
        return compteAdapter.listAllComptes();
    }
}
