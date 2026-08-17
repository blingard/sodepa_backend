package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.ImmoOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * UseCase pour récupérer les détails d'une immobilisation par son identifiant.
 */
@Service
@RequiredArgsConstructor
public class GetImmoByIdUseCase implements UseCase<UUID, ImmoOutput> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public ImmoOutput execute(UUID input) {
        return immobilisationAdapter.getImmoById(input);
    }
}
