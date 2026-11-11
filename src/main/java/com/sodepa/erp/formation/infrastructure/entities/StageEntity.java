package com.sodepa.erp.formation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `StageEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "rh_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "STG";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ codeDossier : code dossier utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "code_dossier", nullable = false, unique = true, length = 60)
    private String codeDossier;

    /**
     * Champ nomPrenomStagiaire : nom prenom stagiaire utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "nom_prenom_stagiaire", nullable = false, length = 150)
    private String nomPrenomStagiaire;

    /**
     * Champ etablissementOrigine : etablissement origine utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "etablissement_origine", length = 150)
    private String etablissementOrigine;

    /**
     * Champ niveauEtude : niveau etude utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "niveau_etude", length = 100)
    private String niveauEtude;

    /**
     * Champ typeStage : type stage utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_stage", nullable = false, length = 40)
    private TypeStage typeStage;

    /**
     * Champ structureAccueil : structure accueil utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "structure_accueil", nullable = false, length = 150)
    private String structureAccueil;

    /**
     * Nom ou désignation textuelle de encadreur pour l'affichage et la traçabilité.
     */
    @Column(name = "encadreur_nom", length = 150)
    private String encadreurNom;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /**
     * Champ lettreAdmissionRef : lettre admission ref utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "lettre_admission_ref", length = 100)
    private String lettreAdmissionRef;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutStage statut = StatutStage.EN_COURS;

    /**
     * Champ dateDemandeRallonge : date demande rallonge utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "date_demande_rallonge")
    private LocalDate dateDemandeRallonge;

    /**
     * Champ rallongeAccordee : rallonge accordee utilisé pour le pilotage et le suivi dans StageEntity.
     */
    @Column(name = "rallonge_accordee", nullable = false)
    @Builder.Default
    private boolean rallongeAccordee = false;
}
