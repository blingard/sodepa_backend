package com.sodepa.erp.configuration;

import com.sodepa.erp.share.*;
import com.sodepa.erp.utils.UserManagementEnginePort;
import com.sodepa.erp.utils.UserOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
public class FilterBaseMethod {
    private final JwtDecoder jwtDecoder;
    private final UserManagementEnginePort userManagementEnginePort;


    public UserData getUser(String token){
        Jwt jwt = jwtDecoder.decode(token);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return extractUserData(jwt);
    }

    /**
     * Authentifie l'utilisateur avec le JWT
     */
    public void authenticateWithJwt(String token) {
        Jwt jwt = jwtDecoder.decode(token);

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        UserData userData = extractUserData(jwt);
        String principal = getPrincipalFromJwt(jwt, GlobalConstants.PREFERENCE_USERNAME);

        CurrentUserAuthenticationToken authentication = new CurrentUserAuthenticationToken(
                principal,
                jwt,
                authorities,
                userData
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("User authenticated: {}", principal);
    }

    /**
     * Extrait les authorities (rôles) du JWT
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extraction des rôles depuis realm_access (Keycloak)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Collection<String> realmRoles = Collections.emptyList();

        if (realmAccess != null && realmAccess.get("roles") != null) {
            realmRoles = (Collection<String>) realmAccess.get("roles");
        }

        return realmRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Extrait les données utilisateur du JWT
     */
    private UserData extractUserData(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        String userId = claims.get(GlobalConstants.USER_ID) != null
                ? claims.get(GlobalConstants.USER_ID).toString()
                : null;
        assert userId != null;
        UserOutput userOutput = userManagementEnginePort.getUserById(UUID.fromString(userId));
        Set<PosteDto> postes = userManagementEnginePort.getUserAllActivePosteByUserId(UUID.fromString(userId));
        Set<PosteData> posteData = postes.stream()
                .map(PosteData::fromDto)
                .collect(Collectors.toSet());
        return new UserData(
                userOutput.username(),
                userOutput.nom()+" "+userOutput.prenom(),
                userOutput.id().toString(),
                userOutput.telephones(),
                userOutput.email(),
                userOutput.permissions(),
                getPrincipalFromJwt(jwt, "sid"),
                jwt.getTokenValue(),
                jwt.getSubject(),
                posteData
        );
    }


    private String getPrincipalFromJwt(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null && Objects.equals(claimName, GlobalConstants.PREFERENCE_USERNAME)) {
            return jwt.getSubject();
        }
        return value;
    }

}
