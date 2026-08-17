package com.sodepa.erp.authentication.application.usecase;

import com.sodepa.erp.authentication.infrastructure.adapter.AuthenticationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour la suppression d'une session.
 */
@Service
@RequiredArgsConstructor
public class DeleteSessionUseCase implements UseCase<String, Void> {
    
    private final AuthenticationAdapter authenticationAdapter;

    @Override
    public Void execute(String sessionId) {
        authenticationAdapter.deleteSession(sessionId);
        return null;
    }
}
