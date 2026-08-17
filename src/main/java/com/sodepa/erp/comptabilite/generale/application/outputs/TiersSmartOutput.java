package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.TypeTiers;
import java.util.UUID;

/**
 * Vue allégée d'un tiers pour les listes et sélections.
 */
public record TiersSmartOutput(
        UUID id,
        String code,
        String raisonSociale,
        TypeTiers typeTiers
) {}
