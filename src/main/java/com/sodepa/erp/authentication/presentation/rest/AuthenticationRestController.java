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
import com.sodepa.erp.share.password.StrongPassword;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.sodepa.erp.share.UtilsService;

import java.util.List;
import java.util.Map;

/**
 * Service de Gestion de l'Authentification, des Sessions et de la Sécurité des Accès (Module Authentification).
 * <p>
 * Ce service constitue la porte d'entrée sécurisée de l'ERP SODEPA reliée au serveur d'identité Keycloak :
 * <ul>
 *   <li><b>Authentification des collaborateurs</b> : Vérification des identifiants (nom d'utilisateur, mot de passe) et émission des jetons sécurisés JWT.</li>
 *   <li><b>Renouvellement de session</b> : Rafraîchissement transparent du jeton d'accès sans obliger l'agent à ressaisir son mot de passe.</li>
 *   <li><b>Déconnexion et révocation</b> : Clôture propre des sessions et invalidation des jetons lors du départ de l'utilisateur.</li>
 *   <li><b>Contrôle des sessions actives concurrentes</b> : Supervision des terminaux connectés et possibilité de fermer à distance une session suspecte.</li>
 *   <li><b>Gestion de la politique de mot de passe fort</b> : Imposition de critères stricts de robustesse pour protéger les comptes financiers et administratifs.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification & Sessions Utilisateurs", description = "Connexion, rafraîchissement des jetons JWT, gestion des sessions actives et changement de mot de passe")
public class AuthenticationRestController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ListSessionsUseCase listSessionsUseCase;
    private final DeleteSessionUseCase deleteSessionUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UtilsService utilsService;

    /**
     * Authentifie un collaborateur de la SODEPA à l'aide de son identifiant et de son mot de passe.
     * <p>
     * Vérifie la validité du compte auprès de Keycloak et renvoie le jeton d'accès (access token JWT),
     * le jeton de rafraîchissement (refresh token), ainsi que les rôles et périmètres de l'agent.
     *
     * @param request identifiants de connexion fournis par l'utilisateur
     * @return la charge utile d'authentification avec jetons et durée de validité
     */
    @PostMapping("/login")
    @Operation(summary = "Authentifier un utilisateur et obtenir les jetons de session")
    public Map<String, Object> login(@RequestBody @Valid LoginRequest request) {
        return loginUseCase.execute(new LoginInput(request.username(), request.password()));
    }

    /**
     * Prolonge la session de travail en renouvelant le jeton d'accès expiré à l'aide d'un jeton de rafraîchissement valide.
     *
     * @param request demande contenant le jeton de rafraîchissement
     * @return le nouveau jeton d'accès actualisé
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renouveler le jeton de session expiré (Refresh Token)")
    public Map<String, Object> refresh(@RequestBody @Valid RefreshRequest request) {
        return refreshTokenUseCase.execute(new RefreshInput(request.refreshToken()));
    }

    /**
     * Clôture formellement la session de travail de l'utilisateur et révoque son jeton.
     *
     * @param request demande de déconnexion contenant le jeton à révoquer
     */
    @PostMapping("/logout")
    @Operation(summary = "Se déconnecter et révoquer la session active")
    public void logout(@RequestBody @Valid LogoutRequest request) {
        logoutUseCase.execute(new LogoutInput(request.refreshToken()));
    }

    /**
     * Liste l'ensemble des sessions de travail ouvertes et actives de l'agent connecté.
     * <p>
     * Affiche les terminaux (ordinateurs, tablettes de terrain en ranch, mobiles)
     * actuellement authentifiés sur le compte de l'utilisateur.
     *
     * @return la liste des sessions actives avec adresses IP et horodatages de connexion
     */
    @GetMapping("/sessions")
    @Operation(summary = "Lister les sessions de travail actives de l'utilisateur connecté")
    public List<SessionOutput> listSessions() {
        return listSessionsUseCase.execute(utilsService.getCurrentUserData().iamId());
    }

    /**
     * Ferme à distance une session de travail spécifique de l'utilisateur (ex: déconnexion d'un poste laissé ouvert).
     *
     * @param sessionId identifiant unique de la session à interrompre
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Fermer à distance une session de travail spécifique")
    public void deleteSession(@PathVariable @Valid @NotBlank(message = "SESSION_ID_IS_BLANK") String sessionId) {
        deleteSessionUseCase.execute(sessionId);
    }

    /**
     * Modifie le mot de passe de l'utilisateur connecté en appliquant la politique de sécurité des mots de passe forts.
     *
     * @param request nouveau mot de passe répondant aux contraintes de complexité (majuscules, chiffres, caractères spéciaux)
     */
    @PostMapping("/change-password")
    @Operation(summary = "Changer le mot de passe de l'utilisateur connecté")
    public void changePassword(@RequestBody @Valid @StrongPassword ChangePasswordRequest request) {
        changePasswordUseCase.execute(
                new ChangePasswordInput(utilsService.getCurrentUserData().iamId(), request.password())
        );
    }
}
