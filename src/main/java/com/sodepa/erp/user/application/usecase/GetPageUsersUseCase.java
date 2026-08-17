package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour paginer les utilisateurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPageUsersUseCase implements UseCase<Pageable, PageRecord<UserOutput>> {

    private final UserAdapter userAdapter;

    @Override
    public PageRecord<UserOutput> execute(Pageable input) {
        log.info("Récupération de la page d'utilisateurs");
        return userAdapter.getPage(input);
    }
}
