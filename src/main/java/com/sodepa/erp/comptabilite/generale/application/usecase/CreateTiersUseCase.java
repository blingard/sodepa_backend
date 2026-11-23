package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateTiersInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `CreateTiersUseCase` au sein de la SODEPA.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CreateTiersUseCase implements UseCase<CreateTiersInput, SubmissionOutput> {
    private final TiersAdapter tiersAdapter;

    @Override
    public SubmissionOutput execute(CreateTiersInput input) {
        log.info("Création d'un nouveau tiers avec le code: {}", input.code());
        return tiersAdapter.initCreateTiers(input);
    }
}
