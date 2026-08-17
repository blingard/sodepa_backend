package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateTiersInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UpdateTiersUseCase implements UseCase<UpdateTiersInput, Void> {
    private final TiersAdapter tiersAdapter;

    @Override
    public Void execute(UpdateTiersInput input) {
        log.info("Mise à jour du tiers avec l'ID: {}", input.id());
        tiersAdapter.updateTiers(input);
        return null;
    }
}
