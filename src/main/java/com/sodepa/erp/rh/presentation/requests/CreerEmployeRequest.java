package com.sodepa.erp.rh.presentation.requests;

import com.sodepa.erp.rh.infrastructure.entities.Sexe;
import com.sodepa.erp.rh.infrastructure.entities.SituationMatrimoniale;
import com.sodepa.erp.rh.infrastructure.entities.TypeContrat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerEmployeRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotNull(message = "Le sexe est obligatoire")
    private Sexe sexe;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    private String lieuNaissance;

    @NotNull(message = "La situation matrimoniale est obligatoire")
    private SituationMatrimoniale situationMatrimoniale;

    @Builder.Default
    private Integer nombreEnfantsACharge = 0;

    private String email;
    private String telephone;
    private String adresse;
    private String numeroCnps;
    private String compteBancaireRib;
    private String banqueNom;

    @NotNull(message = "Le type de contrat est obligatoire")
    private TypeContrat typeContrat;

    @NotNull(message = "La date d'embauche est obligatoire")
    private LocalDate dateEmbauche;

    private LocalDate dateFinEssai;
    private LocalDate dateFinContrat;

    private UUID uniteOrganisationnelleId;
    private UUID posteId;
    private UUID utilisateurId;

    @NotNull(message = "La catégorie est obligatoire")
    private Integer categorie;

    @NotBlank(message = "L'échelon est obligatoire")
    private String echelon;
}
