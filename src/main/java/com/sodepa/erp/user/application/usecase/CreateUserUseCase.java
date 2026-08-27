package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.inputs.CreateUserInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour initier la création d'un utilisateur.
 *
 * <p>
 * Rend l'accusé de soumission : l'appelant n'a rien créé, il a déposé une
 * demande, et il lui faut son identifiant pour en suivre le sort.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateUserUseCase implements UseCase<CreateUserInput, SubmissionOutput> {

    private final UserAdapter userAdapter;

    @Override
    public SubmissionOutput execute(CreateUserInput input) {
        log.info("Exécution du cas d'utilisation pour créer un utilisateur");
        return userAdapter.initCreateUser(input);
    }
}
