package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.TypeTiers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête pour la création d'un tiers.
 */
public record CreateTiersRequest(
        @NotBlank String code,
        @NotBlank String raisonSociale,
        String adresse,
        String telephone,
        @Email String email,
        @NotNull TypeTiers typeTiers,
        @NotBlank String compteCollectifCode
) {}
