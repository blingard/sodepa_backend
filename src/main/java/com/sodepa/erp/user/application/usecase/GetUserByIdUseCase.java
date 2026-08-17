package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cas d'utilisation pour récupérer un utilisateur par ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserByIdUseCase implements UseCase<UUID, UserOutput> {

    private final UserAdapter userAdapter;

    @Override
    public UserOutput execute(UUID id) {
        log.info("Récupération de l'utilisateur avec l'ID {}", id);
        return userAdapter.getUserOutputById(id);
    }
}
