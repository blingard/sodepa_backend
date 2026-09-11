package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import com.sodepa.erp.utils.UseCase;
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
public class GetUserProfileUseCase implements UseCase<Void, UserOutput> {

    private final UserAdapter userAdapter;

    @Override
    public UserOutput execute(Void id) {
        log.info("Récupération du profil de l'utilisateur");
        return userAdapter.getUserOutPutProfile();
    }
}
