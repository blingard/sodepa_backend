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
public class CreerExpressionBesoinRequest {

    private UUID demandeurId;

    @Size(max = 100, message = "ASKER_NAME_REQUIRED_MAX_LENGTH_100")
    private String demandeurNom;

    @NotNull(message = "UNITE_ORGANISATIONNELLE_ID_REQUIRED")
    private UUID uniteOrganisationnelleId;

    @Size(max = 100, message = "ORGANIZATIONAL_UNIT_NAME_REQUIRED_MAX_LENGTH_100")
    private String structureNom;

    @Size(max = 100, min = 10, message = "REASON_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
    private String motif;

    @Size(max = 100, min = 10, message = "JUSTIFICATION_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
    private String ligneBudgetaireImputation;

    @NotEmpty(message = "LINES_REQUIRED")
    private List<LigneExpressionBesoinRequest> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneExpressionBesoinRequest {
        @Size(max = 100, min = 10, message = "DESIGNATION_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
        private String designation;

        @Size(max = 100, min = 10, message = "TECHNICAL_SPECIFICATIONS_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
        private String specificationsTechniques;

        @Positive(message = "QUANTITY_REQUIRED_POSITIVE")
        private BigDecimal quantite;

        @Size(max = 100, min = 10, message = "UNIT_OF_MEASURE_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
        private String uniteMesure;

        @Positive(message = "ESTIMATED_UNIT_PRICE_REQUIRED_POSITIVE")
        private BigDecimal prixUnitaireEstime;
    }
}
