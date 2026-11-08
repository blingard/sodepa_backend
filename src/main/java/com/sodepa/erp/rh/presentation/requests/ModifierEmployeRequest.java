package com.sodepa.erp.rh.presentation.requests;

import com.sodepa.erp.rh.infrastructure.entities.SituationMatrimoniale;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import com.sodepa.erp.rh.infrastructure.entities.TypeContrat;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ModifierEmployeRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierEmployeRequest {
    /**
     * Nom officiel dans le cadre des opérations SODEPA.
     */
    private String nom;
    /**
     * Prénom de la personne dans le cadre des opérations SODEPA.
     */
    private String prenom;
    /**
     * Champ situationMatrimoniale : situation matrimoniale utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private SituationMatrimoniale situationMatrimoniale;
    /**
     * Champ nombreEnfantsACharge : nombre enfants a charge utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private Integer nombreEnfantsACharge;
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
     * Champ numeroCnps : numero cnps utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private String numeroCnps;
    /**
     * Champ compteBancaireRib : compte bancaire rib utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private String compteBancaireRib;
    /**
     * Nom ou désignation textuelle de banque pour l'affichage et la traçabilité.
     */
    private String banqueNom;
    /**
     * Champ typeContrat : type contrat utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private TypeContrat typeContrat;
    /**
     * Champ statutActivite : statut activite utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private StatutActivite statutActivite;
    /**
     * Champ dateFinEssai : date fin essai utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private LocalDate dateFinEssai;
    /**
     * Champ dateFinContrat : date fin contrat utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
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
     * Catégorie de rattachement administratif ou technique dans le cadre des opérations SODEPA.
     */
    private Integer categorie;
    /**
     * Champ echelon : echelon utilisé pour le pilotage et le suivi dans ModifierEmployeRequest.
     */
    private String echelon;
}
