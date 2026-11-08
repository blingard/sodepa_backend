package com.sodepa.erp.rh.presentation.requests;

import com.sodepa.erp.rh.infrastructure.entities.Sexe;
import com.sodepa.erp.rh.infrastructure.entities.SituationMatrimoniale;
import com.sodepa.erp.rh.infrastructure.entities.TypeContrat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerEmployeRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerEmployeRequest {

    /**
     * Nom officiel dans le cadre des opérations SODEPA.
     */
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    /**
     * Prénom de la personne dans le cadre des opérations SODEPA.
     */
    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    /**
     * Sexe de l'animal (Mâle, Femelle) dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "Le sexe est obligatoire")
    private Sexe sexe;

    /**
     * Champ dateNaissance : date naissance utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    /**
     * Champ lieuNaissance : lieu naissance utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    private String lieuNaissance;

    /**
     * Champ situationMatrimoniale : situation matrimoniale utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    @NotNull(message = "La situation matrimoniale est obligatoire")
    private SituationMatrimoniale situationMatrimoniale;

    /**
     * Champ nombreEnfantsACharge : nombre enfants a charge utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    @Builder.Default
    private Integer nombreEnfantsACharge = 0;

    /**
     * Adresse électronique professionnelle de contact dans le cadre des opérations SODEPA.
     */
    private String email;
    /**
     * Numéro de téléphone de contact dans le cadre des opérations SODEPA.
     */
    private String telephone;
    /**
     * Adresse géographique ou postale dans le cadre des opérations SODEPA.
     */
    private String adresse;
    /**
     * Champ numeroCnps : numero cnps utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    private String numeroCnps;
    /**
     * Champ compteBancaireRib : compte bancaire rib utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    private String compteBancaireRib;
    /**
     * Nom ou désignation textuelle de banque pour l'affichage et la traçabilité.
     */
    private String banqueNom;

    /**
     * Champ typeContrat : type contrat utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    @NotNull(message = "Le type de contrat est obligatoire")
    private TypeContrat typeContrat;

    /**
     * Champ dateEmbauche : date embauche utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    @NotNull(message = "La date d'embauche est obligatoire")
    private LocalDate dateEmbauche;

    /**
     * Champ dateFinEssai : date fin essai utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    private LocalDate dateFinEssai;
    /**
     * Champ dateFinContrat : date fin contrat utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    private LocalDate dateFinContrat;

    /**
     * Identifiant unique technique (UUID) de l'élément uniteorganisationnelle associé.
     */
    private UUID uniteOrganisationnelleId;
    /**
     * Identifiant unique technique (UUID) de l'élément poste associé.
     */
    private UUID posteId;
    /**
     * Identifiant unique technique (UUID) de l'élément utilisateur associé.
     */
    private UUID utilisateurId;

    /**
     * Catégorie de rattachement administratif ou technique dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "La catégorie est obligatoire")
    private Integer categorie;

    /**
     * Champ echelon : echelon utilisé pour le pilotage et le suivi dans CreerEmployeRequest.
     */
    @NotBlank(message = "L'échelon est obligatoire")
    private String echelon;
}
