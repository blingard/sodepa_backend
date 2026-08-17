package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.TiersOutput;
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
public class GetTiersByIdUseCase implements UseCase<UUID, TiersOutput> {
    private final TiersAdapter tiersAdapter;

    @Override
    public TiersOutput execute(UUID input) {
        log.info("Récupération des détails du tiers avec l'ID: {}", input);
        return tiersAdapter.getTiersById(input);
    }
}
