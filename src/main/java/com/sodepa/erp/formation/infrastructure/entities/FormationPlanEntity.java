package com.sodepa.erp.formation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité de persistance représentant `FormationPlanEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "rh_formation_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationPlanEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "FPL";

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
     * Champ anneeExercice : annee exercice utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(name = "annee_exercice", nullable = false, unique = true)
    private Integer anneeExercice;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 200)
    private String intitule;

    /**
     * Champ budgetTotalPrevu : budget total prevu utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(name = "budget_total_prevu", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetTotalPrevu = BigDecimal.ZERO;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutPlanFormation statut = StatutPlanFormation.BROUILLON;

    /**
     * Champ visaSdrh : visa sdrh utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(name = "visa_sdrh", nullable = false)
    @Builder.Default
    private boolean visaSdrh = false;

    /**
     * Champ visaDaf : visa daf utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(name = "visa_daf", nullable = false)
    @Builder.Default
    private boolean visaDaf = false;

    /**
     * Champ approbationDg : approbation dg utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(name = "approbation_dg", nullable = false)
    @Builder.Default
    private boolean approbationDg = false;

    /**
     * Champ dateApprobationDg : date approbation dg utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(name = "date_approbation_dg")
    private LocalDate dateApprobationDg;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @Column(columnDefinition = "TEXT")
    private String observations;

    /**
     * Champ actions : actions utilisé pour le pilotage et le suivi dans FormationPlanEntity.
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormationActionEntity> actions = new ArrayList<>();
}
