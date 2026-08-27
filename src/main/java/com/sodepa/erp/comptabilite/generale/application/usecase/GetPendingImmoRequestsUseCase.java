package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation listant les demandes d'immobilisation en attente.
 *
 * <p>
 * C'est la boîte de réception du checker. Sans elle, l'identifiant de demande
 * rendu à la soumission était la seule voie vers
 * {@code validate_or_reject} — qui l'exige en chemin — et une demande dont on
 * avait perdu l'accusé restait à jamais en attente.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GetPendingImmoRequestsUseCase
        implements UseCase<Pageable, PageRecord<MakerCheckerSmartOutput>> {

    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public PageRecord<MakerCheckerSmartOutput> execute(Pageable pageable) {
        return immobilisationAdapter.findPendingRequests(pageable);
    }
}
