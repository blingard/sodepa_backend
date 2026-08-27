package com.sodepa.erp.share;

import com.sodepa.erp.utils.Permissions;
import lombok.Builder;

import java.util.Set;

@Builder
public record UserData(
    String username,
    String name,
    String userId,
    Set<String> phoneNumbers,
    String email,
    Set<Permissions> permissions,
    String sessionId,
    String jwtToken,
    /**
     * Identifiant du compte dans Keycloak (revendication `sub`, colonne
     * `iam`). C'est lui qu'attend l'API d'administration Keycloak — le
     * `userId` ci-dessus est la clé métier, les deux ne sont pas
     * interchangeables.
     */
    String iamId
) {

}
