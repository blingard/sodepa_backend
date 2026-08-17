package com.sodepa.erp.authentication.application.usecase;

import com.sodepa.erp.authentication.application.inputs.LoginInput;
import com.sodepa.erp.authentication.infrastructure.adapter.AuthenticationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Cas d'utilisation pour la connexion.
 */
@Service
@RequiredArgsConstructor
public class LoginUseCase implements UseCase<LoginInput, Map<String, Object>> {
    
    private final AuthenticationAdapter authenticationAdapter;

    @Override
    public Map<String, Object> execute(LoginInput input) {
        return authenticationAdapter.login(input);
    }
}
