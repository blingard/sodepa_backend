package com.sodepa.erp.authentication.presentation.rest;

import com.sodepa.erp.authentication.application.inputs.ChangePasswordInput;
import com.sodepa.erp.authentication.application.inputs.LoginInput;
import com.sodepa.erp.authentication.application.inputs.LogoutInput;
import com.sodepa.erp.authentication.application.inputs.RefreshInput;
import com.sodepa.erp.authentication.application.outputs.SessionOutput;
import com.sodepa.erp.authentication.application.usecase.*;
import com.sodepa.erp.authentication.presentation.requests.ChangePasswordRequest;
import com.sodepa.erp.authentication.presentation.requests.LoginRequest;
import com.sodepa.erp.authentication.presentation.requests.LogoutRequest;
import com.sodepa.erp.authentication.presentation.requests.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour l'authentification.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationRestController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ListSessionsUseCase listSessionsUseCase;
    private final DeleteSessionUseCase deleteSessionUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    /**
     * Endpoint de connexion.
     * @param request les identifiants
     * @return la réponse de connexion
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid LoginRequest request) {
        return loginUseCase.execute(new LoginInput(request.username(), request.password()));
    }

    /**
     * Endpoint de rafraîchissement du jeton.
     * @param request la demande de rafraîchissement
     * @return la réponse avec le nouveau jeton
     */
    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody @Valid RefreshRequest request) {
        return refreshTokenUseCase.execute(new RefreshInput(request.refreshToken()));
    }

    /**
     * Endpoint de déconnexion.
     * @param request la demande de déconnexion
     */
    @PostMapping("/logout")
    public void logout(@RequestBody @Valid LogoutRequest request) {
        logoutUseCase.execute(new LogoutInput(request.refreshToken()));
    }

    /**
     * Endpoint pour lister les sessions actives de l'utilisateur courant.
     * @param jwt le jeton d'authentification de l'utilisateur
     * @return la liste des sessions
     */
    @GetMapping("/sessions")
    public List<SessionOutput> listSessions(@AuthenticationPrincipal Jwt jwt) {
        return listSessionsUseCase.execute(jwt.getSubject());
    }

    /**
     * Endpoint pour supprimer une session spécifique.
     * @param sessionId l'identifiant de la session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        deleteSessionUseCase.execute(sessionId);
    }

    /**
     * Endpoint pour changer le mot de passe de l'utilisateur courant.
     * @param jwt le jeton d'authentification de l'utilisateur
     * @param request la demande de changement de mot de passe
     */
    @PostMapping("/change-password")
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid ChangePasswordRequest request) {
        changePasswordUseCase.execute(new ChangePasswordInput(jwt.getSubject(), request.newPassword()));
    }
}
