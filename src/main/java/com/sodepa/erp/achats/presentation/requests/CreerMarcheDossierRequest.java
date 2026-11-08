package com.sodepa.erp.achats.presentation.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerMarcheDossierRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerMarcheDossierRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément expressionbesoin associé.
     */
    private UUID expressionBesoinId;

    /**
     * Identifiant unique du fournisseur sélectionné dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "PROVIDER_ID_REQUIRED")
    private UUID fournisseurId;

    /**
     * Champ objet : objet utilisé pour le pilotage et le suivi dans CreerMarcheDossierRequest.
     */
    @NotBlank(message = "CODE_MARCHE_REQUIRED")
    @Size(max = 50, message = "MAX_LENGTH_MARKET_50")
    private String objet;

    /**
     * Code normalisé unique identifiant ligneBudgetaireCode dans le référentiel SODEPA.
     */
    @Size(max = 50, message = "MAX_LENGTH_50")
    private String ligneBudgetaireCode;

    /**
     * Champ delaiLivraisonJours : delai livraison jours utilisé pour le pilotage et le suivi dans CreerMarcheDossierRequest.
     */
    @PositiveOrZero(message = "DELIVERY_TIME_POSITIVE")
    private Integer delaiLivraisonJours;

    /**
     * Champ lignes : lignes utilisé pour le pilotage et le suivi dans CreerMarcheDossierRequest.
     */
    @NotEmpty(message = "ITEMS_REQUIRED")
    private List<LigneMarcheDossierRequest> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneMarcheDossierRequest {
    /**
     * Désignation complète du produit, service ou élément dans le cadre des opérations SODEPA.
     */
        @NotBlank(message = "DESIGNATION_REQUIRED")
        private String designation;
    /**
     * Quantité ou volume exprimé dans l'unité de mesure de référence dans le cadre des opérations SODEPA.
     */
        @Positive(message = "QUANTITY_POSITIVE")
        private BigDecimal quantite;
    /**
     * Unité de mesure réglementaire dans le cadre des opérations SODEPA.
     */
        @Size(max = 20, message = "MAX_LENGTH_20")
        private String uniteMesure;
    /**
     * Valeur financière (prix unitaire ht) exprimée en Francs CFA (XAF).
     */
        @Positive(message = "PRICE_POSITIVE")
        private BigDecimal prixUnitaireHt;
    /**
     * Taux de taxe sur la valeur ajoutée (TVA) dans le cadre des opérations SODEPA.
     */
        @Positive(message = "VAT_RATE_POSITIVE")
        private BigDecimal tauxTva;
    }
}
