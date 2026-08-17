package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant un plan budgétaire annuel.
 * Intègre le versionnage et le workflow de validation (Draft -> Submitted -> Published).
 */
@Entity
@Table(name = "budget_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetPlanEntity {

    /**
     * Identifiant unique du plan budgétaire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Exercice comptable concerné par le budget (ex: 2026).
     */
    @Column(nullable = false)
    private Integer annee;

    /**
     * Intitulé ou description du plan budgétaire.
     */
    @Column(nullable = false)
    private String intitule;

    /**
     * Numéro de version du budget pour l'audit trail (ex: 1, 2).
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * Statut actuel du budget au sein du workflow de gouvernance.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutBudget statut;

    /**
     * Total global cumulé des enveloppes planifiées.
     */
    @Column(name = "total_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBudget;

    /**
     * Date de création du plan budgétaire.
     */
    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    /**
     * Identifiant de l'utilisateur ayant créé le plan budgétaire.
     */
    @Column(name = "cree_par")
    private UUID creePar;

    /**
     * Date de la dernière modification du plan.
     */
    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;

    /**
     * Identifiant de l'utilisateur ayant effectué la dernière modification.
     */
    @Column(name = "modifie_par")
    private UUID modifiePar;

    /**
     * Version technique pour la gestion de la concurrence optimiste.
     */
    @Version
    @Column(name = "optimistic_version", nullable = false)
    private Integer optimisticVersion;

    /**
     * Liste des lignes budgétaires détaillées rattachées à ce plan.
     */
    @OneToMany(mappedBy = "budgetPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<BudgetItemEntity> items = new ArrayList<>();

    /**
     * Associe un item budgétaire de manière bidirectionnelle.
     * 
     * @param item l'item à ajouter
     */
    public void addItem(BudgetItemEntity item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setBudgetPlan(this);
    }
}
