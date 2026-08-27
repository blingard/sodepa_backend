package com.sodepa.erp.authentication.infrastructure.adapter;

import com.sodepa.erp.authentication.application.inputs.LoginInput;
import com.sodepa.erp.authentication.application.inputs.LogoutInput;
import com.sodepa.erp.authentication.application.inputs.RefreshInput;
import com.sodepa.erp.authentication.application.outputs.SessionOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import jakarta.ws.rs.core.Response;
import com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort;
import com.sodepa.erp.share.erreurs.AuthentificationIndisponibleException;
import com.sodepa.erp.share.erreurs.IdentifiantsRefusesException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptateur d'infrastructure pour l'authentification via Keycloak.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationAdapter implements KeycloakProvisioningPort {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate;

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Value("${keycloak.default-password}")
    private String defaultPassword;

    /**
     * Authentifie l'utilisateur.
     * @param input les données de connexion
     * @return la réponse contenant les jetons
     */
    public Map<String, Object> login(LoginInput input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "password");
        body.add("username", input.username());
        body.add("password", input.password());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return demanderJetons(url, request, "connexion de " + input.username());
    }

    /**
     * Rafraîchit le jeton.
     * @param input les données de rafraîchissement
     * @return la réponse contenant les nouveaux jetons
     */
    public Map<String, Object> refreshToken(RefreshInput input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", input.refreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return demanderJetons(url, request, "rafraîchissement de jeton");
    }

    /**
     * Appelle le point d'entrée « token » de Keycloak et traduit ses refus.
     *
     * Sans ce filtre, le 401 que Keycloak renvoie sur un mot de passe erroné ou
     * un jeton expiré remonte en {@link HttpClientErrorException}, qu'aucun
     * gestionnaire n'attrape : le client reçoit alors un 500. Un identifiant
     * incorrect devient indistinguable d'une panne, et le front n'a aucun moyen
     * de savoir qu'il doit redemander une connexion.
     *
     * @param url point d'entrée « token » du realm
     * @param request corps du grant, déjà formé
     * @param operation libellé journalisé, pour situer l'échec
     * @return la charge utile des jetons
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> demanderJetons(
            String url, HttpEntity<MultiValueMap<String, String>> request, String operation) {
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return (Map<String, Object>) response.getBody();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            // Keycloak rend 401 sur `invalid_client` et 400 sur `invalid_grant` :
            // les deux désignent un refus d'authentification, pas une panne.
            log.warn("Échec d'authentification Keycloak ({}) : {}", operation, e.getStatusText());
            throw new IdentifiantsRefusesException();
        } catch (RestClientException e) {
            // Keycloak injoignable, TLS, délai dépassé : là, c'est bien une panne,
            // mais elle mérite un 503 et une trace, pas un 500 muet.
            log.error("Keycloak injoignable ({})", operation, e);
            throw new AuthentificationIndisponibleException();
        }
    }

    /**
     * Déconnecte l'utilisateur.
     * @param input les données de déconnexion
     */
    public void logout(LogoutInput input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", input.refreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        restTemplate.postForEntity(url, request, String.class);
    }

    /**
     * Liste les sessions actives de l'utilisateur.
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des sessions
     */
    public List<SessionOutput> listSessions(String userId) {
        List<UserSessionRepresentation> sessions = keycloak.realm(realm)
                .users()
                .get(userId)
                .getUserSessions();

        return sessions.stream()
                .map(session -> SessionOutput.builder()
                        .id(session.getId())
                        .username(session.getUsername())
                        .ipAddress(session.getIpAddress())
                        .start(session.getStart())
                        .lastAccess(session.getLastAccess())
                        .clients(session.getClients())
                        .build())
                .toList();
    }

    /**
     * Supprime une session spécifique.
     * @param sessionId l'identifiant de la session
     */
    public void deleteSession(String sessionId) {
        keycloak.realm(realm).deleteSession(sessionId, false);
    }

    @Override
    public UUID createKeycloakUser(UUID id, String username, String email, String firstName, String lastName, boolean active) {
        try {
            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setUsername(username);
            kcUser.setLastName(lastName);
            kcUser.setFirstName(firstName);
            kcUser.setEmail(email);
            kcUser.setEmailVerified(true);
            kcUser.setEnabled(active);
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("USER_ID", List.of(id.toString()));
            kcUser.setAttributes(attributes);

            CredentialRepresentation creds = new CredentialRepresentation();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue(defaultPassword);
            creds.setTemporary(false);
            kcUser.setCredentials(List.of(creds));

            try (Response response = keycloak.realm(realm).users().create(kcUser)) {
                if (response.getStatus() >= 400 && response.getStatus() != 409) {
                    String errorMsg = response.readEntity(String.class);
                    log.error("Erreur de création d'utilisateur dans Keycloak: status={}, body={}", response.getStatus(), errorMsg);
                    throw new RuntimeException("Erreur de création Keycloak: " + errorMsg);
                }
                UUID createdId = extractCreatedUserId(response, username);
                log.info("Utilisateur créé dans Keycloak avec succès. username: {}", username);
                return createdId;
            }
        } catch (Exception e) {
            log.error("Exception lors de la création de l'utilisateur dans Keycloak: ", e);
            throw new RuntimeException("Erreur d'intégration Keycloak : " + e.getMessage(), e);
        }
    }

    /**
     * Extrait l'identifiant Keycloak du compte créé, depuis l'en-tête Location.
     *
     * Sur un conflit (409 : le compte existe déjà) Keycloak ne renvoie pas
     * d'en-tête Location. On retrouve alors le compte par son nom
     * d'utilisateur : renvoyer un identifiant de repli inventé donnerait une
     * colonne `iam` qui ne désigne rien.
     */
    private UUID extractCreatedUserId(Response response, String username) {
        URI location = response.getLocation();
        if (location == null) {
            return retrouverParNomUtilisateur(username);
        }
        String path = location.getPath();
        String extractedId = path.substring(path.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(extractedId);
        } catch (IllegalArgumentException e) {
            log.warn("Impossible de parser l'ID retourné par Keycloak dans le header Location: {}", extractedId);
            return retrouverParNomUtilisateur(username);
        }
    }

    /**
     * Retrouve un compte Keycloak par son nom d'utilisateur, en correspondance
     * exacte.
     */
    private UUID retrouverParNomUtilisateur(String username) {
        List<UserRepresentation> trouves =
                keycloak.realm(realm).users().search(username, true);
        if (trouves == null || trouves.isEmpty()) {
            throw new RuntimeException(
                    "Keycloak n'a renvoyé aucun identifiant pour l'utilisateur " + username);
        }
        return UUID.fromString(trouves.get(0).getId());
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        try {
            CredentialRepresentation creds = new CredentialRepresentation();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue(newPassword);
            creds.setTemporary(false);
            keycloak.realm(realm).users().get(userId).resetPassword(creds);
            log.info("Mot de passe modifié avec succès dans Keycloak pour l'utilisateur ID: {}", userId);
        } catch (Exception e) {
            log.error("Exception lors de la modification du mot de passe dans Keycloak pour l'utilisateur ID: {}", userId, e);
            throw new RuntimeException("Erreur de modification du mot de passe dans Keycloak: " + e.getMessage(), e);
        }
    }

}
