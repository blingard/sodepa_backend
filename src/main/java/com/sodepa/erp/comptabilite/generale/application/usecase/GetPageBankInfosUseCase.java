package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.BankSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.BankAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GetPageBankInfosUseCase implements UseCase<Pageable, PageRecord<BankSmartOutput>> {

    private final BankAdapter bankAdapter;
    @Override
    public PageRecord<BankSmartOutput> execute(Pageable input) {
        log.info("Pageable bank");
        return bankAdapter.getBankByPage(input);
    }
}
