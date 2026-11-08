package com.sodepa.erp.tresorerie.regies.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerDemandeApprovisionnementRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerDemandeApprovisionnementRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément regie associé.
     */
    @NotNull(message = "L'identifiant de la régie est obligatoire")
    private UUID regieId;

    /**
     * Valeur financière (montant demande fcfa) exprimée en Francs CFA (XAF).
     */
    @NotNull(message = "Le montant d'approvisionnement demandé est obligatoire")
    private BigDecimal montantDemandeFcfa;

    /**
     * Champ motifReapprovisionnement : motif reapprovisionnement utilisé pour le pilotage et le suivi dans CreerDemandeApprovisionnementRequest.
     */
    @NotBlank(message = "Le motif du réapprovisionnement est obligatoire")
    private String motifReapprovisionnement;

    /**
     * Champ etatJustificatifsDepenses : etat justificatifs depenses utilisé pour le pilotage et le suivi dans CreerDemandeApprovisionnementRequest.
     */
    private String etatJustificatifsDepenses;

    /**
     * Valeur financière (montant justificatifs valides) exprimée en Francs CFA (XAF).
     */
    private BigDecimal montantJustificatifsValides;
}
