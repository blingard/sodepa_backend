package com.sodepa.erp.comptabilite.generale.application.outputs;

import lombok.Builder;
import java.util.UUID;

/**
 * Vue allégée d'un compte pour les sélections.
 */
@Builder
public record CompteSmartOutput(
    UUID id,
    String code,
    String intitule
) {
}
