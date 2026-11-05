package com.sodepa.erp.formation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rh_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageEntity {

    @Id
    private UUID id;

    @Column(name = "code_dossier", nullable = false, unique = true, length = 60)
    private String codeDossier;

    @Column(name = "nom_prenom_stagiaire", nullable = false, length = 150)
    private String nomPrenomStagiaire;

    @Column(name = "etablissement_origine", length = 150)
    private String etablissementOrigine;

    @Column(name = "niveau_etude", length = 100)
    private String niveauEtude;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_stage", nullable = false, length = 40)
    private TypeStage typeStage;

    @Column(name = "structure_accueil", nullable = false, length = 150)
    private String structureAccueil;

    @Column(name = "encadreur_nom", length = 150)
    private String encadreurNom;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "lettre_admission_ref", length = 100)
    private String lettreAdmissionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutStage statut = StatutStage.EN_COURS;

    @Column(name = "date_demande_rallonge")
    private LocalDate dateDemandeRallonge;

    @Column(name = "rallonge_accordee", nullable = false)
    @Builder.Default
    private boolean rallongeAccordee = false;
}
