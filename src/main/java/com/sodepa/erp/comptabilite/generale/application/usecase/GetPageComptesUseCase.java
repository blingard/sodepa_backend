package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.CompteSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.CompteAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * UseCase pour récupérer la liste des comptes par page.
 */
@Service
@RequiredArgsConstructor
public class GetPageComptesUseCase implements UseCase<Pageable, PageRecord<CompteSmartOutput>> {
    private final CompteAdapter compteAdapter;

    @Override
    public PageRecord<CompteSmartOutput> execute(Pageable input) {
        return compteAdapter.getComptesByPage(input);
    }
}
