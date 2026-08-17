package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.BankSmartOutput;
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
public class GetActiveBankByIdUseCase implements UseCase<UUID, BankSmartOutput> {

    private final BankAdapter bankAdapter;
    @Override
    public BankSmartOutput execute(UUID input) {
        log.info("Get active by Id");
        return bankAdapter.getActiveBankById(input);
    }
}
