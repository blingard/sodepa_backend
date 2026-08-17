package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.user.application.inputs.UpdatePermissionsInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour initier la mise à jour des permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserPermissionsUseCase implements UseCase<UpdatePermissionsInput, Void> {

    private final UserAdapter userAdapter;

    @Override
    public Void execute(UpdatePermissionsInput input) {
        log.info("Exécution du cas d'utilisation pour mettre à jour les permissions");
        userAdapter.initUpdatePermissions(input);
        return null;
    }
}
