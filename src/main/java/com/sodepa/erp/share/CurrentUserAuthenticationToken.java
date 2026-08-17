package com.sodepa.erp.share;

import java.util.Collection;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;


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

}
