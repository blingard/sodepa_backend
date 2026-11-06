package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entité JPA représentant une règle d'aiguillage conditionnel entre deux étapes d'un workflow.
 * <p>
 * Permet au moteur de dévier le flux d'approbation séquentiel classique en fonction de critères dynamiques
 * (ex: montant d'engagement > 5 000 000 FCFA requérant l'arbitrage direct du Directeur Général,
 * unité opérationnelle de type Ranch ou Abattoir, criticité budgétaire).
 * </p>
 */
@Entity
@Table(name = "workflows_conditions_transitions", indexes = {
    @Index(name = "idx_wf_trans_source", columnList = "etape_source_id"),
    @Index(name = "idx_wf_trans_cible", columnList = "etape_cible_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowConditionTransitionEntity {

    /**
     * Identifiant unique universel (UUID) de la règle de transition conditionnelle.
     */
    @Id
    private UUID id;

    /**
     * Étape d'origine à partir de laquelle la condition est évaluée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_source_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeSource;

    /**
     * Étape de destination vers laquelle le flux est aiguillé si l'expression s'évalue à vrai.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_cible_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeCible;

    /**
     * Expression logique évaluée dynamiquement contre les métadonnées de contexte (ex: SpEL ou syntaxe d'opérateurs).
     * Exemple : {@code "#montant > 5000000"} ou {@code "#typeUnite == 'RANCH'"}.
     */
    @Column(name = "expression_condition", length = 255)
    private String expressionCondition;

    /**
     * Ordre d'évaluation prioritaire des transitions sortantes de la même étape source.
     * Les conditions de priorité la plus basse (1, puis 2...) sont testées en premier.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priorite = 1;
}
