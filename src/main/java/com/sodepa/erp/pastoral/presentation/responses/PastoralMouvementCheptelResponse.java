package com.sodepa.erp.pastoral.presentation.responses;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralMouvementCheptelEntity;
import com.sodepa.erp.pastoral.infrastructure.entities.TypeMouvementCheptel;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralMouvementCheptelResponse {

    private UUID id;
    private String numeroMouvement;
    private UUID bovinId;
    private String bovinMatricule;
    private UUID ranchSourceId;
    private String ranchSourceNom;
    private UUID ranchDestinationId;
    private String ranchDestinationNom;
    private TypeMouvementCheptel typeMouvement;
    private LocalDate dateMouvement;
    private BigDecimal poidsKg;
    private String ancienneCategorie;
    private String nouvelleCategorie;
    private BigDecimal valeurMouvement;
    private String compteDebitSyscohada;
    private String compteCreditSyscohada;
    private String justificatifRef;
    private String observations;
    private String creePar;
    private Instant dateCreation;

    public static PastoralMouvementCheptelResponse fromEntity(PastoralMouvementCheptelEntity entity) {
        if (entity == null) return null;
        return PastoralMouvementCheptelResponse.builder()
                .id(entity.getId())
                .numeroMouvement(entity.getNumeroMouvement())
                .bovinId(entity.getBovin() != null ? entity.getBovin().getId() : null)
                .bovinMatricule(entity.getBovin() != null ? entity.getBovin().getMatriculeBoucle() : null)
                .ranchSourceId(entity.getRanchSource() != null ? entity.getRanchSource().getId() : null)
                .ranchSourceNom(entity.getRanchSource() != null ? entity.getRanchSource().getNom() : null)
                .ranchDestinationId(entity.getRanchDestination() != null ? entity.getRanchDestination().getId() : null)
                .ranchDestinationNom(entity.getRanchDestination() != null ? entity.getRanchDestination().getNom() : null)
                .typeMouvement(entity.getTypeMouvement())
                .dateMouvement(entity.getDateMouvement())
                .poidsKg(entity.getPoidsKg())
                .ancienneCategorie(entity.getAncienneCategorie())
                .nouvelleCategorie(entity.getNouvelleCategorie())
                .valeurMouvement(entity.getValeurMouvement())
                .compteDebitSyscohada(entity.getCompteDebitSyscohada())
                .compteCreditSyscohada(entity.getCompteCreditSyscohada())
                .justificatifRef(entity.getJustificatifRef())
                .observations(entity.getObservations())
                .creePar(entity.getCreePar())
                .dateCreation(entity.getDateCreation())
                .build();
    }
}
