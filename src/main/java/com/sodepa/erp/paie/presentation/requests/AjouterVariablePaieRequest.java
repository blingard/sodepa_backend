package com.sodepa.erp.paie.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `AjouterVariablePaieRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjouterVariablePaieRequest {

    /**
     * Identifiant unique de l'employé concerné dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "L'employé est obligatoire")
    private UUID employeId;

    /**
     * Champ codeRubrique : code rubrique utilisé pour le pilotage et le suivi dans AjouterVariablePaieRequest.
     */
    @NotBlank(message = "Le code de la rubrique est obligatoire")
    private String codeRubrique;

    /**
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    private String libelle;

    /**
     * Montant financier exprimé en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal montant;

    /**
     * Quantité ou volume exprimé dans l'unité de mesure de référence dans le cadre des opérations SODEPA.
     */
    private BigDecimal quantite;

    /**
     * Commentaires et observations opérationnelles dans le cadre des opérations SODEPA.
     */
    private String commentaire;
}
