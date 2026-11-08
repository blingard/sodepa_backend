package com.sodepa.erp.achats.presentation.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerExpressionBesoinRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerExpressionBesoinRequest {

    /**
     * Identifiant de l'employé initiateur de la demande dans le cadre des opérations SODEPA.
     */
    private UUID demandeurId;

    /**
     * Nom ou désignation textuelle de demandeur pour l'affichage et la traçabilité.
     */
    @Size(max = 100, message = "ASKER_NAME_REQUIRED_MAX_LENGTH_100")
    private String demandeurNom;

    /**
     * Identifiant unique technique (UUID) de l'élément uniteorganisationnelle associé.
     */
    @NotNull(message = "UNITE_ORGANISATIONNELLE_ID_REQUIRED")
    private UUID uniteOrganisationnelleId;

    /**
     * Nom ou désignation textuelle de structure pour l'affichage et la traçabilité.
     */
    @Size(max = 100, message = "ORGANIZATIONAL_UNIT_NAME_REQUIRED_MAX_LENGTH_100")
    private String structureNom;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    @Size(max = 100, min = 10, message = "REASON_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
    private String motif;

    /**
     * Champ ligneBudgetaireImputation : ligne budgetaire imputation utilisé pour le pilotage et le suivi dans CreerExpressionBesoinRequest.
     */
    @Size(max = 100, min = 10, message = "JUSTIFICATION_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
    private String ligneBudgetaireImputation;

    /**
     * Champ lignes : lignes utilisé pour le pilotage et le suivi dans CreerExpressionBesoinRequest.
     */
    @NotEmpty(message = "LINES_REQUIRED")
    private List<LigneExpressionBesoinRequest> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneExpressionBesoinRequest {
    /**
     * Désignation complète du produit, service ou élément dans le cadre des opérations SODEPA.
     */
        @Size(max = 100, min = 10, message = "DESIGNATION_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
        private String designation;

    /**
     * Champ specificationsTechniques : specifications techniques utilisé pour le pilotage et le suivi dans CreerExpressionBesoinRequest.
     */
        @Size(max = 100, min = 10, message = "TECHNICAL_SPECIFICATIONS_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
        private String specificationsTechniques;

    /**
     * Quantité ou volume exprimé dans l'unité de mesure de référence dans le cadre des opérations SODEPA.
     */
        @Positive(message = "QUANTITY_REQUIRED_POSITIVE")
        private BigDecimal quantite;

    /**
     * Unité de mesure réglementaire dans le cadre des opérations SODEPA.
     */
        @Size(max = 100, min = 10, message = "UNIT_OF_MEASURE_REQUIRED_MIN_LENGTH_10_MAX_LENGTH_100")
        private String uniteMesure;

    /**
     * Valeur financière (prix unitaire estime) exprimée en Francs CFA (XAF).
     */
        @Positive(message = "ESTIMATED_UNIT_PRICE_REQUIRED_POSITIVE")
        private BigDecimal prixUnitaireEstime;
    }
}
