package com.sodepa.erp.utils;


import com.sodepa.erp.share.AffectationPosteDto;
import com.sodepa.erp.share.PosteDto;

import java.util.Set;
import java.util.UUID;

/**
 * Composant applicatif `UserManagementEnginePort` du système ERP SODEPA.
 */
public interface UserManagementEnginePort {
    record AuthContext(UserOutput user, Set<PosteDto> activePostes) {}

    UserOutput getUserById(UUID id);
    Set<PosteDto> getUserAllActivePosteByUserId(UUID id);
    Set<PosteDto> getUserAllPostByUserId(UUID id);
    Set<Permissions> resolveEffectivePermissions(UUID userId);

    default AuthContext resolveAuthContext(UUID userId) {
        return new AuthContext(getUserById(userId), getUserAllActivePosteByUserId(userId));
    }

    default void evictAuthCache(UUID userId) {
        // Implémenté par les moteurs disposant d'un cache d'autorisation
    }
}
