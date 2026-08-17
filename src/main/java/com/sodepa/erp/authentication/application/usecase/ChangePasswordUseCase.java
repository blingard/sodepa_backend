package com.sodepa.erp.authentication.application.usecase;

import com.sodepa.erp.authentication.application.inputs.ChangePasswordInput;
import com.sodepa.erp.authentication.infrastructure.adapter.AuthenticationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour le changement de mot de passe d'un utilisateur.
 */
@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements UseCase<ChangePasswordInput, Void> {

    private final AuthenticationAdapter authenticationAdapter;

    @Override
    public Void execute(ChangePasswordInput input) {
        authenticationAdapter.changePassword(input.userId(), input.newPassword());
        return null;
    }
}
