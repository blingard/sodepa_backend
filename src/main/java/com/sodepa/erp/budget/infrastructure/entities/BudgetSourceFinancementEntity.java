package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Source de financement budgétaire SODEPA (F-REF-018 / CA-REF-018-01) :
 * Fonds propres SODEPA, Subvention MINEPIA / BIP, Bailleurs internationaux (PRODEL / Banque Mondiale), Emprunts.
 */
@Entity
@Table(name = "budget_sources_financement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetSourceFinancementEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "BSF";

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
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Intitule dans le contexte du traitement BudgetSourceFinancementEntity.
     */
    @Column(nullable = false, length = 180)
    private String intitule;

    /**
     * Type : FONDS_PROPRES, SUBVENTION_ETAT, FINANCEMENT_EXTERIEUR, EMPRUNT.
     */
    @Column(name = "type_source", nullable = false, length = 40)
    private String typeSource;

    /**
     * Bailleur nom dans le contexte du traitement BudgetSourceFinancementEntity.
     */
    @Column(name = "bailleur_nom", length = 150)
    private String bailleurNom;

    /**
     * Devise dans le contexte du traitement BudgetSourceFinancementEntity.
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String devise = "XAF";

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
