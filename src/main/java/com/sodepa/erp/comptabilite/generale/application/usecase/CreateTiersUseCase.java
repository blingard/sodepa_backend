package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateTiersInput;
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
public class CreateTiersUseCase implements UseCase<CreateTiersInput, Void> {
    private final TiersAdapter tiersAdapter;

    @Override
    public Void execute(CreateTiersInput input) {
        log.info("Création d'un nouveau tiers avec le code: {}", input.code());
        tiersAdapter.initCreateTiers(input);
        return null;
    }
}
