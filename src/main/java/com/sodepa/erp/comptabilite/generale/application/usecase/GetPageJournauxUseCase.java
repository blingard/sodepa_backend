package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.JournalSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Cas d'utilisation pour lister les journaux paginés.
 */
@Component
@RequiredArgsConstructor
public class GetPageJournauxUseCase implements UseCase<Pageable, PageRecord<JournalSmartOutput>> {

    private final JournalAdapter journalAdapter;

    @Override
    public PageRecord<JournalSmartOutput> execute(Pageable input) {
        return journalAdapter.getJournauxByPage(input);
    }
}
