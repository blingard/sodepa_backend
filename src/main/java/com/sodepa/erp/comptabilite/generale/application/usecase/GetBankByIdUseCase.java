package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.BankOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.BankAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GetBankByIdUseCase implements UseCase<UUID, BankOutput> {

    private final BankAdapter bankAdapter;
    @Override
    public BankOutput execute(UUID input) {
        log.info("Get by Id");
        return bankAdapter.getBankById(input);
    }
}
