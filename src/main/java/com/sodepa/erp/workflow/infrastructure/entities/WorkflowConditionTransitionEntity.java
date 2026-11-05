package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entité de transition conditionnelle entre deux étapes (règle d'aiguillage).
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

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_source_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_cible_id", nullable = false)
    private WorkflowEtapeDefinitionEntity etapeCible;

    @Column(name = "expression_condition", length = 255)
    private String expressionCondition;

    @Column(nullable = false)
    @Builder.Default
    private Integer priorite = 1;
}
