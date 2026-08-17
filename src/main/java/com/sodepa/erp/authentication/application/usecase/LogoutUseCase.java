package com.sodepa.erp.authentication.application.usecase;

import com.sodepa.erp.authentication.application.inputs.LogoutInput;
import com.sodepa.erp.authentication.infrastructure.adapter.AuthenticationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour la déconnexion.
 */
@Service
@RequiredArgsConstructor
public class LogoutUseCase implements UseCase<LogoutInput, Void> {
    
    private final AuthenticationAdapter authenticationAdapter;

    @Override
    public Void execute(LogoutInput input) {
        authenticationAdapter.logout(input);
        return null;
    }
}
