package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.BankSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.BankAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ListBankSmartInfosUseCase implements UseCase<Void, Set<BankSmartOutput>> {

    private final BankAdapter bankAdapter;
    @Override
    public Set<BankSmartOutput> execute(Void input) {
        log.info("List all bank");
        return bankAdapter.listAllActiveBank();
    }
}
