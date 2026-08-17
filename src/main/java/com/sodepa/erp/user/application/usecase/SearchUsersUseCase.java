package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.user.application.inputs.SearchUsersInput;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour la recherche des utilisateurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchUsersUseCase implements UseCase<SearchUsersInput, PageRecord<UserOutput>> {

    private final UserAdapter userAdapter;

    @Override
    public PageRecord<UserOutput> execute(SearchUsersInput input) {
        log.info("Recherche des utilisateurs avec les critères : {}", input);
        return userAdapter.search(input);
    }
}
