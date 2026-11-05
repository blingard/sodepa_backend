package com.sodepa.erp.achats.presentation.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerMarcheDossierRequest {

    private UUID expressionBesoinId;

    @NotNull(message = "PROVIDER_ID_REQUIRED")
    private UUID fournisseurId;

    @NotBlank(message = "CODE_MARCHE_REQUIRED")
    @Size(max = 50, message = "MAX_LENGTH_MARKET_50")
    private String objet;

    @Size(max = 50, message = "MAX_LENGTH_50")
    private String ligneBudgetaireCode;

    @PositiveOrZero(message = "DELIVERY_TIME_POSITIVE")
    private Integer delaiLivraisonJours;

    @NotEmpty(message = "ITEMS_REQUIRED")
    private List<LigneMarcheDossierRequest> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneMarcheDossierRequest {
        @NotBlank(message = "DESIGNATION_REQUIRED")
        private String designation;
        @Positive(message = "QUANTITY_POSITIVE")
        private BigDecimal quantite;
        @Size(max = 20, message = "MAX_LENGTH_20")
        private String uniteMesure;
        @Positive(message = "PRICE_POSITIVE")
        private BigDecimal prixUnitaireHt;
        @Positive(message = "VAT_RATE_POSITIVE")
        private BigDecimal tauxTva;
    }
}
