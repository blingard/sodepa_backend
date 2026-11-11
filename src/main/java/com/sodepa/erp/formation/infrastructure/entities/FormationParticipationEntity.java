package com.sodepa.erp.formation.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `FormationParticipationEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "rh_formation_participations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationParticipationEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "FP";

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
     * Action métier ou technique déclenchée dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private FormationActionEntity action;

    /**
     * Employé ou collaborateur SODEPA concerné dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    /**
     * Champ noteEvaluationChaud : note evaluation chaud utilisé pour le pilotage et le suivi dans FormationParticipationEntity.
     */
    @Column(name = "note_evaluation_chaud", precision = 4, scale = 2)
    private BigDecimal noteEvaluationChaud;

    /**
     * Champ attestationDelivree : attestation delivree utilisé pour le pilotage et le suivi dans FormationParticipationEntity.
     */
    @Column(name = "attestation_delivree", nullable = false)
    @Builder.Default
    private boolean attestationDelivree = false;

    /**
     * Champ avisHierarchique : avis hierarchique utilisé pour le pilotage et le suivi dans FormationParticipationEntity.
     */
    @Column(name = "avis_hierarchique", length = 50)
    @Builder.Default
    private String avisHierarchique = "FAVORABLE";
}
