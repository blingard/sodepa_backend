package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une exécution de workflow sur un document / objet métier précis.
 */
@Entity
@Table(name = "workflows_instances", indexes = {
    @Index(name = "idx_wf_inst_entity", columnList = "entity_name, entity_id"),
    @Index(name = "idx_wf_inst_statut", columnList = "statut"),
    @Index(name = "idx_wf_inst_initiateur", columnList = "initiateur_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinitionEntity workflowDefinition;

    @Column(name = "entity_name", nullable = false, length = 60)
    private String entityName;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiateur_id", nullable = false)
    private UtilisateurEntity initiateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutWorkflowInstance statut = StatutWorkflowInstance.EN_COURS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_courante_id")
    private WorkflowEtapeDefinitionEntity etapeCourante;

    @Column(name = "donnees_contexte", columnDefinition = "TEXT")
    private String donneesContexte;

    @Column(name = "date_initiation", nullable = false)
    private Instant dateInitiation;

    @Column(name = "date_cloture")
    private Instant dateCloture;

    @Builder.Default
    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTacheEntity> taches = new ArrayList<>();
}
