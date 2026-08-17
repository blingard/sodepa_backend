package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.TiersSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetActiveTiersByIdUseCase implements UseCase<UUID, TiersSmartOutput> {
    private final TiersAdapter tiersAdapter;

    @Override
    public TiersSmartOutput execute(UUID input) {
        log.info("Récupération du tiers actif avec l'ID: {}", input);
        return tiersAdapter.getActiveTiersById(input);
    }
}
