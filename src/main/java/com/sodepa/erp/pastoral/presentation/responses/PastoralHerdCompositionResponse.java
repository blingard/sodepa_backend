package com.sodepa.erp.pastoral.presentation.responses;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralHerdCompositionEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `PastoralHerdCompositionResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralHerdCompositionResponse {

    private UUID id;
    private Integer anneeExercice;
    private UUID ranchId;
    private String ranchNom;
    private LocalDate dateInventaire;
    private Integer effectifPhysiqueTotal;
    private Integer effectifVeaux;
    private Integer effectifGenissesTaurillons;
    private Integer effectifReproducteurs;
    private Integer effectifEmboucheReforme;
    private BigDecimal valeurTotaleCheptel;
    private String statutRapprochement;
    private String observations;

    public static PastoralHerdCompositionResponse fromEntity(PastoralHerdCompositionEntity entity) {
        if (entity == null) return null;
        return PastoralHerdCompositionResponse.builder()
                .id(entity.getId())
                .anneeExercice(entity.getAnneeExercice())
                .ranchId(entity.getRanch() != null ? entity.getRanch().getId() : null)
                .ranchNom(entity.getRanch() != null ? entity.getRanch().getNom() : null)
                .dateInventaire(entity.getDateInventaire())
                .effectifPhysiqueTotal(entity.getEffectifPhysiqueTotal())
                .effectifVeaux(entity.getEffectifVeaux())
                .effectifGenissesTaurillons(entity.getEffectifGenissesTaurillons())
                .effectifReproducteurs(entity.getEffectifReproducteurs())
                .effectifEmboucheReforme(entity.getEffectifEmboucheReforme())
                .valeurTotaleCheptel(entity.getValeurTotaleCheptel())
                .statutRapprochement(entity.getStatutRapprochement())
                .observations(entity.getObservations())
                .build();
    }
}
