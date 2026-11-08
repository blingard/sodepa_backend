package com.sodepa.erp.tresorerie.regies.presentation.requests;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerDepenseRegieRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerDepenseRegieRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément regie associé.
     */
    @NotNull(message = "L'identifiant de la régie est obligatoire")
    private UUID regieId;

    /**
     * Champ dateDepense : date depense utilisé pour le pilotage et le suivi dans EnregistrerDepenseRegieRequest.
     */
    private LocalDate dateDepense;

    /**
     * Champ beneficiaire : beneficiaire utilisé pour le pilotage et le suivi dans EnregistrerDepenseRegieRequest.
     */
    @NotBlank(message = "Le bénéficiaire est obligatoire")
    private String beneficiaire;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    @NotBlank(message = "Le motif de la dépense est obligatoire")
    private String motif;

    /**
     * Champ imputationBudgetaire : imputation budgetaire utilisé pour le pilotage et le suivi dans EnregistrerDepenseRegieRequest.
     */
    private String imputationBudgetaire;

    /**
     * Valeur financière (montant depense fcfa) exprimée en Francs CFA (XAF).
     */
    @NotNull(message = "Le montant de la dépense est obligatoire")
    @DecimalMax(value = "100000.00", message = "Les menues dépenses de régie sont plafonnées à 100 000 FCFA par opération")
    private BigDecimal montantDepenseFcfa;

    /**
     * Champ justificatifRef : justificatif ref utilisé pour le pilotage et le suivi dans EnregistrerDepenseRegieRequest.
     */
    private String justificatifRef;
}
