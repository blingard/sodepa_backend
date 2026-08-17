package com.sodepa.erp.budget.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un engagement de dépenses (réservation budgétaire avant saisie de l'écriture réelle).
 */
@Entity
@Table(name = "budget_engagement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetEngagementEntity {

    /**
     * Identifiant unique de l'engagement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Ligne budgétaire sur laquelle s'impute l'engagement.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private BudgetItemEntity budgetItem;

    /**
     * Numéro d'engagement unique (ex: numéro de bon de commande).
     */
    @Column(name = "numero_engagement", nullable = false, unique = true, length = 100)
    private String numeroEngagement;

    /**
     * Description ou motif de la dépense engagée.
     */
    @Column(length = 255)
    private String description;

    /**
     * Montant de la dépense engagée.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Date et heure de l'engagement budgétaire.
     */
    @Column(name = "date_engagement", nullable = false)
    private LocalDateTime dateEngagement;

    /**
     * Statut de l'engagement (ex: 'ENGAGED', 'CONVERTED_TO_REAL', 'CANCELLED').
     */
    @Column(nullable = false, length = 50)
    private String statut;

    /**
     * Statut de workflow d'approbation (ex: 'PRE_ENGAGEMENT', 'APPROVED_BY_CHEF', 'APPROVED_BY_DF', 'APPROVED_BY_DG').
     */
    @Column(name = "statut_workflow", length = 100)
    private String statutWorkflow;

    /**
     * Rôle de l'approbateur courant (ex: 'CHEF_SERVICE', 'DIRECTEUR_FINANCIER', 'DIRECTEUR_GENERAL').
     */
    @Column(name = "approbateur_courant_role", length = 100)
    private String approbateurCourantRole;
}
