package com.sodepa.erp.comptabilite.generale.infrastructure.event;

import com.sodepa.erp.utils.TypeTiers;
import java.util.UUID;

/**
 * Payload sérialisé dans le système Maker-Checker pour les opérations sur les tiers.
 */
public record TiersEventInput(
        UUID id,
        String code,
        String raisonSociale,
        String adresse,
        String telephone,
        String email,
        TypeTiers typeTiers,
        Boolean actif,
        String compteCollectifCode,
        String userId
) {}
