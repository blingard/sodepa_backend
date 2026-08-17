package com.sodepa.erp.authentication.application.usecase;

import com.sodepa.erp.authentication.application.inputs.RefreshInput;
import com.sodepa.erp.authentication.infrastructure.adapter.AuthenticationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Cas d'utilisation pour le rafraîchissement du jeton.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase implements UseCase<RefreshInput, Map<String, Object>> {
    
    private final AuthenticationAdapter authenticationAdapter;

    @Override
    public Map<String, Object> execute(RefreshInput input) {
        return authenticationAdapter.refreshToken(input);
    }
}
