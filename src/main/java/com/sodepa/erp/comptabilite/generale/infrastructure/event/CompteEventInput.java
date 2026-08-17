package com.sodepa.erp.comptabilite.generale.infrastructure.event;

import lombok.Builder;
import java.util.UUID;

/**
 * Payload sérialisé dans le système Maker-Checker pour les opérations sur les comptes.
 */
@Builder
public record CompteEventInput(
    UUID id,
    String code,
    String intitule,
    String parentCode,
    Integer niveau,
    String typeAnalytique,
    String nature,
    Boolean isAuxiliaire,
    String userId
) {
}
