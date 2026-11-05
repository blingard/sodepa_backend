package com.sodepa.erp.workflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant un circuit de validation (modèle de workflow).
 */
@Entity
@Table(name = "workflows_definitions", indexes = {
    @Index(name = "idx_wf_def_code", columnList = "code", unique = true),
    @Index(name = "idx_wf_def_domaine", columnList = "domaine")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 150)
    private String intitule;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkflowDomaine domaine;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Builder.Default
    @OneToMany(mappedBy = "workflowDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<WorkflowEtapeDefinitionEntity> etapes = new ArrayList<>();

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;
}
