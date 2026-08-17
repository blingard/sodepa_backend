package com.sodepa.erp.authentication.application.outputs;

import lombok.Builder;
import java.util.Map;

/**
 * Résultat représentant une session utilisateur.
 */
@Builder
public record SessionOutput(
    String id,
    String username,
    String ipAddress,
    long start,
    long lastAccess,
    Map<String, String> clients
) {}
