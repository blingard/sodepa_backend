package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation listant les demandes d'utilisateur en attente de décision.
 *
 * <p>
 * C'est la boîte de réception du checker. Sans elle, l'identifiant de demande
 * généré à la soumission n'était exposé nulle part, et
 * {@code validate_or_reject} — qui l'exige en chemin — restait inappelable :
 * aucune demande ne pouvait plus jamais être tranchée.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPendingUserRequestsUseCase
        implements UseCase<Pageable, PageRecord<MakerCheckerSmartOutput>> {

    private final UserAdapter userAdapter;

    @Override
    public PageRecord<MakerCheckerSmartOutput> execute(Pageable pageable) {
        log.info("Listing des demandes utilisateur en attente");
        return userAdapter.findPendingRequests(pageable);
    }
}
