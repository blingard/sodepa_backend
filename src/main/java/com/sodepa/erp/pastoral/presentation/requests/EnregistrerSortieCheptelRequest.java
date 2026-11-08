package com.sodepa.erp.pastoral.presentation.requests;

import com.sodepa.erp.pastoral.infrastructure.entities.TypeMouvementCheptel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerSortieCheptelRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerSortieCheptelRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément bovin associé.
     */
    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    @NotNull(message = "Le type de sortie est obligatoire")
    private TypeMouvementCheptel typeSortie; // MORTALITE, VENTE_ABATTAGE, REFORME

    /**
     * Champ dateSortie : date sortie utilisé pour le pilotage et le suivi dans EnregistrerSortieCheptelRequest.
     */
    private LocalDate dateSortie;

    /**
     * Valeur financière (montant cession) exprimée en Francs CFA (XAF).
     */
    private BigDecimal montantCession;

    /**
     * Champ justificatifRef : justificatif ref utilisé pour le pilotage et le suivi dans EnregistrerSortieCheptelRequest.
     */
    private String justificatifRef;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans EnregistrerSortieCheptelRequest.
     */
    private String observations;
}
