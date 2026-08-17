package com.sodepa.erp.authentication.application.usecase;

import com.sodepa.erp.authentication.application.outputs.SessionOutput;
import com.sodepa.erp.authentication.infrastructure.adapter.AuthenticationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cas d'utilisation pour la récupération de la liste des sessions.
 */
@Service
@RequiredArgsConstructor
public class ListSessionsUseCase implements UseCase<String, List<SessionOutput>> {
    
    private final AuthenticationAdapter authenticationAdapter;

    @Override
    public List<SessionOutput> execute(String userId) {
        return authenticationAdapter.listSessions(userId);
    }
}
