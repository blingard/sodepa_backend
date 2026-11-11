package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Activité opérationnelle rattachée à un programme budgétaire SODEPA (F-REF-018 / CA-REF-018-01).
 */
@Entity
@Table(name = "budget_activites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetActiviteEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "BA";

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
     * Intitule dans le contexte du traitement BudgetActiviteEntity.
     */
    @Column(nullable = false, length = 180)
    private String intitule;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "programme_code", nullable = false, length = 40)
    private String programmeCode;

    /**
     * Identifiant unique.
     */
    @Column(name = "unite_organisationnelle_id")
    private UUID uniteOrganisationnelleId;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
