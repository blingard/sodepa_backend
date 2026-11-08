package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerActionFormationRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerActionFormationRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément plan associé.
     */
    @NotNull(message = "L'identifiant du plan est obligatoire")
    private UUID planId;

    /**
     * Champ theme : theme utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    @NotBlank(message = "Le thème de formation est obligatoire")
    private String theme;

    /**
     * Champ domaine : domaine utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    @NotBlank(message = "Le domaine est obligatoire")
    private String domaine;

    /**
     * Champ publicCible : public cible utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    private String publicCible;

    /**
     * Champ organismeFormateur : organisme formateur utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    private String organismeFormateur;

    /**
     * Champ typeFormateur : type formateur utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    private String typeFormateur;

    /**
     * Champ lieu : lieu utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    private String lieu;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    private LocalDate dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    private LocalDate dateFin;

    /**
     * Champ dureeHeures : duree heures utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    private Integer dureeHeures;

    /**
     * Valeur financière (cout estimatif fcfa) exprimée en Francs CFA (XAF).
     */
    private BigDecimal coutEstimatifFcfa;

    /**
     * Champ donneDroitReclassement : donne droit reclassement utilisé pour le pilotage et le suivi dans CreerActionFormationRequest.
     */
    private boolean donneDroitReclassement;
}
