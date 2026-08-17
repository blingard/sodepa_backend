package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.TypeTiers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entrée pour la création d'un tiers (couche applicative).
 */
public record CreateTiersInput(
        @NotBlank String code,
        @NotBlank String raisonSociale,
        String adresse,
        String telephone,
        @Email String email,
        @NotNull TypeTiers typeTiers,
        @NotBlank String compteCollectifCode
) {}
