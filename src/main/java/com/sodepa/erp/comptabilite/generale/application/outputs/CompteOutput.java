package com.sodepa.erp.comptabilite.generale.application.outputs;

import lombok.Builder;
import java.util.UUID;

/**
 * Vue complète d'un compte comptable.
 */
@Builder
public record CompteOutput(
    UUID id,
    String code,
    String intitule,
    String parentCode,
    Integer niveau,
    String typeAnalytique,
    String nature,
    Boolean isAuxiliaire
) {
}
