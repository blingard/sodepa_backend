package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.inputs.UpdateUserInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour initier la mise à jour d'un utilisateur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserUseCase implements UseCase<UpdateUserInput, Void> {

    private final UserAdapter userAdapter;

    @Override
    public Void execute(UpdateUserInput input) {
        log.info("Exécution du cas d'utilisation pour mettre à jour un utilisateur");
        userAdapter.initUpdateUser(input);
        return null;
    }
}
