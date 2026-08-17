package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.inputs.ChangePhotoInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour changer la photo de profil d'un utilisateur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePhotoUseCase implements UseCase<ChangePhotoInput, Void> {

    private final UserAdapter userAdapter;

    @Override
    public Void execute(ChangePhotoInput input) {
        log.info("Exécution du cas d'utilisation pour changer la photo de profil");
        userAdapter.initChangePhoto(input);
        return null;
    }
}
