package com.sodepa.erp.formation.presentation.requests;

import com.sodepa.erp.formation.infrastructure.entities.TypeStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerStagiaireRequest {

    @NotBlank(message = "Le nom et prénom du stagiaire sont obligatoires")
    private String nomPrenomStagiaire;

    private String etablissementOrigine;

    private String niveauEtude;

    @NotNull(message = "Le type de stage est obligatoire")
    private TypeStage typeStage;

    @NotBlank(message = "La structure d'accueil est obligatoire")
    private String structureAccueil;

    private String encadreurNom;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private String lettreAdmissionRef;
}
