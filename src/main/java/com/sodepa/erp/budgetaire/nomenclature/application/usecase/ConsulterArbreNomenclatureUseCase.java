package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.nomenclature.application.outputs.NoeudNomenclatureOutput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Arbre de la nomenclature ; l'entrée dit s'il faut inclure les éléments désactivés. */
@Service
@RequiredArgsConstructor
public class ConsulterArbreNomenclatureUseCase implements UseCase<Boolean, List<NoeudNomenclatureOutput>> {

    private final NomenclatureAdapter adapter;

    @Override
    public List<NoeudNomenclatureOutput> execute(Boolean input) {
        return adapter.arbre(Boolean.TRUE.equals(input));
    }
}
