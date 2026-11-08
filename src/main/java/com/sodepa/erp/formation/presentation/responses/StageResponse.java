package com.sodepa.erp.formation.presentation.responses;

import com.sodepa.erp.formation.infrastructure.entities.StageEntity;
import com.sodepa.erp.formation.infrastructure.entities.StatutStage;
import com.sodepa.erp.formation.infrastructure.entities.TypeStage;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `StageResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageResponse {

    private UUID id;
    private String codeDossier;
    private String nomPrenomStagiaire;
    private String etablissementOrigine;
    private String niveauEtude;
    private TypeStage typeStage;
    private String structureAccueil;
    private String encadreurNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lettreAdmissionRef;
    private StatutStage statut;
    private LocalDate dateDemandeRallonge;
    private boolean rallongeAccordee;

    public static StageResponse fromEntity(StageEntity entity) {
        if (entity == null) return null;
        return StageResponse.builder()
                .id(entity.getId())
                .codeDossier(entity.getCodeDossier())
                .nomPrenomStagiaire(entity.getNomPrenomStagiaire())
                .etablissementOrigine(entity.getEtablissementOrigine())
                .niveauEtude(entity.getNiveauEtude())
                .typeStage(entity.getTypeStage())
                .structureAccueil(entity.getStructureAccueil())
                .encadreurNom(entity.getEncadreurNom())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .lettreAdmissionRef(entity.getLettreAdmissionRef())
                .statut(entity.getStatut())
                .dateDemandeRallonge(entity.getDateDemandeRallonge())
                .rallongeAccordee(entity.isRallongeAccordee())
                .build();
    }
}
