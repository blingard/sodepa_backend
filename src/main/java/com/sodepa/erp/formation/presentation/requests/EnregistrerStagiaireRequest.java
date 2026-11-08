package com.sodepa.erp.formation.presentation.requests;

import com.sodepa.erp.formation.infrastructure.entities.TypeStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerStagiaireRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerStagiaireRequest {

    /**
     * Champ nomPrenomStagiaire : nom prenom stagiaire utilisé pour le pilotage et le suivi dans EnregistrerStagiaireRequest.
     */
    @NotBlank(message = "Le nom et prénom du stagiaire sont obligatoires")
    private String nomPrenomStagiaire;

    /**
     * Champ etablissementOrigine : etablissement origine utilisé pour le pilotage et le suivi dans EnregistrerStagiaireRequest.
     */
    private String etablissementOrigine;

    /**
     * Champ niveauEtude : niveau etude utilisé pour le pilotage et le suivi dans EnregistrerStagiaireRequest.
     */
    private String niveauEtude;

    /**
     * Champ typeStage : type stage utilisé pour le pilotage et le suivi dans EnregistrerStagiaireRequest.
     */
    @NotNull(message = "Le type de stage est obligatoire")
    private TypeStage typeStage;

    /**
     * Champ structureAccueil : structure accueil utilisé pour le pilotage et le suivi dans EnregistrerStagiaireRequest.
     */
    @NotBlank(message = "La structure d'accueil est obligatoire")
    private String structureAccueil;

    /**
     * Nom ou désignation textuelle de encadreur pour l'affichage et la traçabilité.
     */
    private String encadreurNom;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    /**
     * Champ lettreAdmissionRef : lettre admission ref utilisé pour le pilotage et le suivi dans EnregistrerStagiaireRequest.
     */
    private String lettreAdmissionRef;
}
