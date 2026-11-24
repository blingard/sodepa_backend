package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.nomenclature.application.outputs.ElementProgrammatiqueOutput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Fiche d'un élément de la nomenclature. */
@Service
@RequiredArgsConstructor
public class ConsulterElementUseCase implements UseCase<UUID, ElementProgrammatiqueOutput> {

    private final NomenclatureAdapter adapter;

    @Override
    public ElementProgrammatiqueOutput execute(UUID input) {
        return adapter.consulter(input);
    }
}
