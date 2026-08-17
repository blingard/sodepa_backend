package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DownloadFecUseCase implements UseCase<Integer, String> {

    private final FecUseCase fecUseCase;

    @Override
    public String execute(Integer annee) {
        return fecUseCase.genererFec(annee);
    }
}
