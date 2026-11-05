package com.sodepa.erp.formation.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rh_formation_participations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationParticipationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private FormationActionEntity action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    @Column(name = "note_evaluation_chaud", precision = 4, scale = 2)
    private BigDecimal noteEvaluationChaud;

    @Column(name = "attestation_delivree", nullable = false)
    @Builder.Default
    private boolean attestationDelivree = false;

    @Column(name = "avis_hierarchique", length = 50)
    @Builder.Default
    private String avisHierarchique = "FAVORABLE";
}
