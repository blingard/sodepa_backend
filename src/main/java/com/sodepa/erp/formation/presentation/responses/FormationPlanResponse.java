package com.sodepa.erp.formation.presentation.responses;

import com.sodepa.erp.formation.infrastructure.entities.FormationPlanEntity;
import com.sodepa.erp.formation.infrastructure.entities.StatutPlanFormation;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Composant applicatif `FormationPlanResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationPlanResponse {

    private UUID id;
    private Integer anneeExercice;
    private String intitule;
    private BigDecimal budgetTotalPrevu;
    private StatutPlanFormation statut;
    private boolean visaSdrh;
    private boolean visaDaf;
    private boolean approbationDg;
    private LocalDate dateApprobationDg;
    private String observations;
    private List<FormationActionResponse> actions;

    public static FormationPlanResponse fromEntity(FormationPlanEntity entity) {
        if (entity == null) return null;
        return FormationPlanResponse.builder()
                .id(entity.getId())
                .anneeExercice(entity.getAnneeExercice())
                .intitule(entity.getIntitule())
                .budgetTotalPrevu(entity.getBudgetTotalPrevu())
                .statut(entity.getStatut())
                .visaSdrh(entity.isVisaSdrh())
                .visaDaf(entity.isVisaDaf())
                .approbationDg(entity.isApprobationDg())
                .dateApprobationDg(entity.getDateApprobationDg())
                .observations(entity.getObservations())
                .actions(entity.getActions() != null
                        ? entity.getActions().stream().map(FormationActionResponse::fromEntity).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
