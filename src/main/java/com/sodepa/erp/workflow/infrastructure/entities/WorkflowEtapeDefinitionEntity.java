package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une étape séquentielle ou conditionnelle au sein d'un workflow.
 */
@Entity
@Table(name = "workflows_etapes_definitions", indexes = {
    @Index(name = "idx_wf_etape_wf", columnList = "workflow_definition_id"),
    @Index(name = "idx_wf_etape_code", columnList = "code_etape")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEtapeDefinitionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinitionEntity workflowDefinition;

    @Column(nullable = false)
    private Integer ordre;

    @Column(name = "code_etape", nullable = false, length = 60)
    private String codeEtape;

    @Column(nullable = false, length = 150)
    private String intitule;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_etape", nullable = false, length = 40)
    private TypeEtapeWorkflow typeEtape;

    @Enumerated(EnumType.STRING)
    @Column(name = "politique_validation", nullable = false, length = 40)
    @Builder.Default
    private PolitiqueValidation politiqueValidation = PolitiqueValidation.UN_SEUL_SUFFIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategie_assignation", nullable = false, length = 40)
    private StrategieAssignation strategieAssignation;

    @Column(name = "valeur_assignation", length = 100)
    private String valeurAssignation;

    @Column(name = "delai_sla_heures")
    private Integer delaiSlaHeures;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_depassement_sla", nullable = false, length = 40)
    @Builder.Default
    private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

    @Builder.Default
    @OneToMany(mappedBy = "etapeSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priorite ASC")
    private List<WorkflowConditionTransitionEntity> transitionsSortantes = new ArrayList<>();
}
