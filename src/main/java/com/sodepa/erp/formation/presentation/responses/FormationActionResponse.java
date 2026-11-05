package com.sodepa.erp.formation.presentation.responses;

import com.sodepa.erp.formation.infrastructure.entities.FormationActionEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationActionResponse {

    private UUID id;
    private UUID planId;
    private String code;
    private String theme;
    private String domaine;
    private String publicCible;
    private String organismeFormateur;
    private String typeFormateur;
    private String lieu;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer dureeHeures;
    private BigDecimal coutEstimatifFcfa;
    private boolean donneDroitReclassement;
    private int nombreParticipants;

    public static FormationActionResponse fromEntity(FormationActionEntity entity) {
        if (entity == null) return null;
        return FormationActionResponse.builder()
                .id(entity.getId())
                .planId(entity.getPlan() != null ? entity.getPlan().getId() : null)
                .code(entity.getCode())
                .theme(entity.getTheme())
                .domaine(entity.getDomaine())
                .publicCible(entity.getPublicCible())
                .organismeFormateur(entity.getOrganismeFormateur())
                .typeFormateur(entity.getTypeFormateur())
                .lieu(entity.getLieu())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .dureeHeures(entity.getDureeHeures())
                .coutEstimatifFcfa(entity.getCoutEstimatifFcfa())
                .donneDroitReclassement(entity.isDonneDroitReclassement())
                .nombreParticipants(entity.getParticipations() != null ? entity.getParticipations().size() : 0)
                .build();
    }
}
