package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.TiersSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPageTiersUseCase implements UseCase<Pageable, PageRecord<TiersSmartOutput>> {
    private final TiersAdapter tiersAdapter;

    @Override
    public PageRecord<TiersSmartOutput> execute(Pageable input) {
        log.info("Récupération de la liste paginée des tiers");
        return tiersAdapter.getTiersByPage(input);
    }
}
