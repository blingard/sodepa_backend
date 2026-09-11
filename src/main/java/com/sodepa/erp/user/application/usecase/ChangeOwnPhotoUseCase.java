package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.inputs.ChangePhotoInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour qu'un utilisateur change sa propre photo de profil.
 * Contrairement à {@link ChangePhotoUseCase} qui passe par le Maker-Checker,
 * ce use case permet une mise à jour directe sans validation tierce.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeOwnPhotoUseCase implements UseCase<ChangePhotoInput, Void> {

    private final UserAdapter userAdapter;

    @Override
    public Void execute(ChangePhotoInput input) {
        log.info("Exécution du cas d'utilisation pour changer sa propre photo de profil");
        userAdapter.changePhoto(input);
        return null;
    }
}
