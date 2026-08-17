package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.TiersSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListTiersUseCase implements UseCase<Void, Set<TiersSmartOutput>> {
    private final TiersAdapter tiersAdapter;

    @Override
    public Set<TiersSmartOutput> execute(Void input) {
        log.info("Récupération de la liste de tous les tiers actifs");
        return tiersAdapter.listAllActiveTiers();
    }
}
