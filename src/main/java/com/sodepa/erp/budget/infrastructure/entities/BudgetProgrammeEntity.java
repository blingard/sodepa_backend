package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Programme budgétaire de la nomencleéature par programme SODEPA (F-REF-018 / CA-REF-018-01).
 */
@Entity
@Table(name = "budget_programmes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetProgrammeEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "BPR";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /**
     * Intitule dans le contexte du traitement BudgetProgrammeEntity.
     */
    @Column(nullable = false, length = 180)
    private String intitule;

    /**
     * Objectif strategique dans le contexte du traitement BudgetProgrammeEntity.
     */
    @Column(name = "objectif_strategique", length = 300)
    private String objectifStrategique;

    /**
     * Responsable hiérarchique ou opérationnel en charge.
     */
    @Column(name = "responsable_programme", length = 150)
    private String responsableProgramme;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
