package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.FiscalYearClosingAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'usage gérant les travaux d'inventaire de fin d'année et la clôture comptable d'un exercice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClotureExerciceUseCase implements UseCase<Integer, Void> {

    private final FiscalYearClosingAdapter fiscalYearClosingAdapter;

    @Override
    public Void execute(Integer annee) {
        fiscalYearClosingAdapter.fiscalYearClosing(annee);
        return null;
    }
}
