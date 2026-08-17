package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.inputs.CreateUserInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour initier la création d'un utilisateur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateUserUseCase implements UseCase<CreateUserInput, Void> {

    private final UserAdapter userAdapter;

    @Override
    public Void execute(CreateUserInput input) {
        log.info("Exécution du cas d'utilisation pour créer un utilisateur");
        userAdapter.initCreateUser(input);
        return null;
    }
}
