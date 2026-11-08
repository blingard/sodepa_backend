package com.sodepa.erp.achats.presentation.requests;

import com.sodepa.erp.achats.infrastructure.entities.ResultatConformitePv;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerPvReceptionRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerPvReceptionRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément dossier associé.
     */
    @NotNull(message = "DOSSIER_ID_REQUIRED")
    private UUID dossierId;

    /**
     * Champ dateReception : date reception utilisé pour le pilotage et le suivi dans EnregistrerPvReceptionRequest.
     */
    @NotNull(message = "RECEPTION_DATE_REQUIRED")
    @PastOrPresent(message = "RECEPTION_DATE_PAST_OR_PRESENT")
    private LocalDate dateReception;

    /**
     * Champ lieuReception : lieu reception utilisé pour le pilotage et le suivi dans EnregistrerPvReceptionRequest.
     */
    @NotNull(message = "RECEPTION_LIEU_REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH_255")
    private String lieuReception;

    /**
     * Champ resultatConformite : resultat conformite utilisé pour le pilotage et le suivi dans EnregistrerPvReceptionRequest.
     */
    @NotNull(message = "RESULTAT_CONFORMITE_REQUIRED")
    private ResultatConformitePv resultatConformite;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans EnregistrerPvReceptionRequest.
     */
    @Size(max = 255, message = "MAX_LENGTH_255")
    private String observations;

    /**
     * Champ numeroFactureDefinitive : numero facture definitive utilisé pour le pilotage et le suivi dans EnregistrerPvReceptionRequest.
     */
    @Size(max = 100, message = "MAX_LENGTH_100")
    private String numeroFactureDefinitive;

    /**
     * Valeur financière (montant facture liquide) exprimée en Francs CFA (XAF).
     */
    @Positive(message = "AMOUNT_POSITIVE")
    private BigDecimal montantFactureLiquide;

    /**
     * Champ fichierPv : fichier pv utilisé pour le pilotage et le suivi dans EnregistrerPvReceptionRequest.
     */
    @NotNull(message = "FICHIER_PV_REQUIRED")
    private MultipartFile fichierPv;
}
