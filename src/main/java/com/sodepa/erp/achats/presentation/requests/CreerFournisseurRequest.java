package com.sodepa.erp.achats.presentation.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerFournisseurRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerFournisseurRequest {

    /**
     * Champ raisonSociale : raison sociale utilisé pour le pilotage et le suivi dans CreerFournisseurRequest.
     */
    @NotBlank(message = "RAISON_SOCIALE_REQUIRED")
    @Size(max = 100, message = "MAX_LENGTH_100")
    private String raisonSociale;

    /**
     * Champ numeroContribuable : numero contribuable utilisé pour le pilotage et le suivi dans CreerFournisseurRequest.
     */
    @Size(max = 100, min = 9, message = "MAX_CONTRIBUABLE_LENGTH_100")
    private String numeroContribuable;

    /**
     * Champ rccm : rccm utilisé pour le pilotage et le suivi dans CreerFournisseurRequest.
     */
    @Size(max = 100, min = 9, message = "MAX_RCCM_LENGTH_100")
    private String rccm;

    /**
     * Champ regimeFiscal : regime fiscal utilisé pour le pilotage et le suivi dans CreerFournisseurRequest.
     */
    @Size(max = 100, min = 9, message = "MAX_REGIME_FISCALE_LENGTH_100")
    private String regimeFiscal;

    /**
     * Champ specialite : specialite utilisé pour le pilotage et le suivi dans CreerFournisseurRequest.
     */
    private String specialite;

    /**
     * Adresse géographique ou postale dans le cadre des opérations SODEPA.
     */
    @Size(max = 100, min = 9, message = "MAX_ADDRESS_LENGTH_100")
    private String adresse;

    /**
     * Numéro de téléphone de contact dans le cadre des opérations SODEPA.
     */
    @Size(max = 25, min = 9, message = "MAX_PHONE_LENGTH_100")
    private String telephone;

    /**
     * Adresse électronique professionnelle de contact dans le cadre des opérations SODEPA.
     */
    @Size(max = 100, min = 9, message = "MAX_EMAIL_LENGTH_100")
    @Email(message = "EMAIL_INVALID")
    private String email;

    /**
     * Nom ou désignation textuelle de banque pour l'affichage et la traçabilité.
     */
    @Size(max = 100, min = 9, message = "MAX_BANK_NAME_LENGTH_100")
    private String banqueNom;

    /**
     * Champ compteBancaireRib : compte bancaire rib utilisé pour le pilotage et le suivi dans CreerFournisseurRequest.
     */
    @Size(max = 100, min = 9, message = "MAX_RIB_LENGTH_100")
    private String compteBancaireRib;
}
