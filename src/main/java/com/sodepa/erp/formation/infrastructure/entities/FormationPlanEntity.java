package com.sodepa.erp.formation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rh_formation_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationPlanEntity {

    @Id
    private UUID id;

    @Column(name = "annee_exercice", nullable = false, unique = true)
    private Integer anneeExercice;

    @Column(nullable = false, length = 200)
    private String intitule;

    @Column(name = "budget_total_prevu", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetTotalPrevu = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutPlanFormation statut = StatutPlanFormation.BROUILLON;

    @Column(name = "visa_sdrh", nullable = false)
    @Builder.Default
    private boolean visaSdrh = false;

    @Column(name = "visa_daf", nullable = false)
    @Builder.Default
    private boolean visaDaf = false;

    @Column(name = "approbation_dg", nullable = false)
    @Builder.Default
    private boolean approbationDg = false;

    @Column(name = "date_approbation_dg")
    private LocalDate dateApprobationDg;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormationActionEntity> actions = new ArrayList<>();
}
