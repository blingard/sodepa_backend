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

/**
 * Composant applicatif `FilterBaseMethod` du système ERP SODEPA.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class FilterBaseMethod {
    private final JwtDecoder jwtDecoder;
    private final UserManagementEnginePort userManagementEnginePort;


    public UserData getUser(String token){
        Jwt jwt = jwtDecoder.decode(token);
        return extractUserData(jwt, userManagementEnginePort.resolveAuthContext(extractUserUuid(jwt)));
    }

    /**
     * Authentifie l'utilisateur avec le JWT
     */
    public void authenticateWithJwt(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        UUID userUuid = extractUserUuid(jwt);

        UserManagementEnginePort.AuthContext authContext;
        try {
            authContext = userManagementEnginePort.resolveAuthContext(userUuid);
        } catch (RuntimeException e) {
            throw new org.springframework.security.oauth2.jwt.JwtException("User referenced by JWT not found or inactive", e);
        }
        if (authContext == null || authContext.user() == null || !authContext.user().actif()) {
            throw new org.springframework.security.oauth2.jwt.JwtException("User referenced by JWT not found or inactive");
        }

        UserData userData = extractUserData(jwt, authContext);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt, userData);
        String principal = getPrincipalFromJwt(jwt, GlobalConstants.PREFERENCE_USERNAME);

        CurrentUserAuthenticationToken authentication = new CurrentUserAuthenticationToken(
                principal,
                jwt,
                authorities,
                userData
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        org.slf4j.MDC.put("userId", userData.userId());
        org.slf4j.MDC.put("username", userData.username());
        log.debug("User authenticated: {}", principal);
    }

    /**
     * Extrait les authorities (rôles Keycloak + rôles métier SODEPA + permissions atomiques)
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt, UserData userData) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> realmRoles) {
            for (Object roleObj : realmRoles) {
                if (roleObj != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleObj.toString().toUpperCase(Locale.ROOT)));
                }
            }
        }

        if (userData != null) {
            if (userData.isAdmin()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            if (userData.isDg()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_DG"));
            }
            if (userData.isDaf()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_DAF"));
            }
            if (userData.posteData() != null) {
                for (PosteData pd : userData.posteData()) {
                    if (pd != null && pd.codePoste() != null && pd.codePoste().toUpperCase(Locale.ROOT).contains("DRH")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_DRH"));
                    }
                }
            }
            if (userData.permissions() != null) {
                for (com.sodepa.erp.utils.Permissions p : userData.permissions()) {
                    if (p != null) {
                        authorities.add(new SimpleGrantedAuthority(p.name()));
                    }
                }
            }
        }
        return new ArrayList<>(authorities);
    }

    private UUID extractUserUuid(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        String userId = claims.get(GlobalConstants.USER_ID) != null
                ? claims.get(GlobalConstants.USER_ID).toString()
                : null;
        if (userId == null || userId.isBlank()) {
            throw new org.springframework.security.oauth2.jwt.JwtException("Missing USER_ID claim in JWT");
        }
        try {
            return UUID.fromString(userId.trim());
        } catch (IllegalArgumentException e) {
            throw new org.springframework.security.oauth2.jwt.JwtException("Invalid USER_ID claim format in JWT", e);
        }
    }

    /**
     * Extrait les données utilisateur du JWT et du contexte d'autorisation
     */
    private UserData extractUserData(Jwt jwt, UserManagementEnginePort.AuthContext authContext) {
        if (authContext == null || authContext.user() == null) {
            throw new org.springframework.security.oauth2.jwt.JwtException("User referenced by JWT not found");
        }
        UserOutput userOutput = authContext.user();
        if (!userOutput.actif()) {
            throw new org.springframework.security.oauth2.jwt.JwtException("Current user is not active");
        }
        Set<PosteDto> postes = authContext.activePostes();
        Set<PosteData> posteData = postes != null
                ? postes.stream().map(PosteData::fromDto).collect(Collectors.toSet())
                : Collections.emptySet();

        boolean isAdmin = "admin_maker".equalsIgnoreCase(userOutput.username())
                || "admin_checker".equalsIgnoreCase(userOutput.username())
                || (userOutput.permissions() != null && userOutput.permissions().size() == com.sodepa.erp.utils.Permissions.values().length);
        boolean isDg = hasRoleOrPoste(jwt, posteData, "DG", "POSTE_DG", "DIRECTEUR GENERAL", "DIRECTEUR GÉNÉRAL");
        boolean isDaf = hasRoleOrPoste(jwt, posteData, "DAF", "POSTE_DIRECTEUR_DAF", "DIRECTEUR ADMINISTRATIF ET FINANCIER");

        return new UserData(
                userOutput.username(),
                userOutput.nom() + " " + userOutput.prenom(),
                userOutput.id().toString(),
                userOutput.telephones(),
                userOutput.email(),
                userOutput.permissions(),
                getPrincipalFromJwt(jwt, "sid"),
                jwt.getTokenValue(),
                jwt.getSubject(),
                posteData,
                isAdmin,
                isDaf,
                isDg
        );
    }

    private boolean hasRoleOrPoste(Jwt jwt, Set<PosteData> posteData, String... markers) {
        if (posteData != null) {
            for (PosteData pd : posteData) {
                String code = pd.codePoste() != null ? pd.codePoste().trim().toUpperCase(Locale.ROOT) : "";
                String title = pd.name() != null ? pd.name().trim().toUpperCase(Locale.ROOT) : "";
                for (String m : markers) {
                    if (code.equals(m) || title.equals(m)) {
                        return true;
                    }
                }
            }
        }
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            for (Object r : roles) {
                if (r != null) {
                    String roleUpper = r.toString().trim().toUpperCase(Locale.ROOT);
                    for (String m : markers) {
                        if (roleUpper.equals(m)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    private String getPrincipalFromJwt(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null && Objects.equals(claimName, GlobalConstants.PREFERENCE_USERNAME)) {
            return jwt.getSubject();
        }
        return value;
    }

}
