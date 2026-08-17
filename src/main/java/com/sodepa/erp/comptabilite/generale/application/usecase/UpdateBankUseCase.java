package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateBankInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateBankInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.BankAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UpdateBankUseCase implements UseCase<UpdateBankInput, Void> {

    private final BankAdapter bankAdapter;
    @Override
    public Void execute(UpdateBankInput input) {
        log.info("Init Bank Update.");
        bankAdapter.updateBank(input);
        return null;
    }
}
