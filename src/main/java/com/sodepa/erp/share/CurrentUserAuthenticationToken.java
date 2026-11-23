package com.sodepa.erp.share;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Composant applicatif `CurrentUserAuthenticationToken` du système ERP SODEPA.
 */
public class CurrentUserAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private Optional<UserData> userData;

    /**
     * Constructeur pour l'authentification non authentifiée (avant validation)
     */
    public CurrentUserAuthenticationToken(Object principal, Object credentials,  UserData userData) {
        super(principal, credentials);
        this.userData = Optional.ofNullable(userData);
    }

    /**
     * Constructeur pour l'authentification authentifiée (après validation)
     */
    public CurrentUserAuthenticationToken(Object principal, Object credentials,
                                          Collection<? extends GrantedAuthority> authorities,
                                          UserData userData) {
        super(principal, credentials, authorities);
        this.userData = Optional.of(userData);
    }

    public Optional<UserData> getUserData() {
        return userData;
    }

    /**
     * Extrait l'UUID de l'utilisateur authentifié depuis le {@link SecurityContextHolder}
     * afin d'empêcher toute usurpation d'identité via un paramètre client.
     */
    public static UUID resolveUserIdOrFallback(UUID fallback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof CurrentUserAuthenticationToken token) {
            Optional<UUID> fromJwt = token.getUserData()
                    .map(UserData::userId)
                    .filter(id -> id != null && !id.isBlank())
                    .map(UUID::fromString);
            if (fromJwt.isPresent()) {
                return fromJwt.get();
            }
        }
        return fallback;
    }

}
