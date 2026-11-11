package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `TresorerieRegieDepenseEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "tresorerie_regie_depenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieDepenseEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "TRD";

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
     * Champ numeroPiece : numero piece utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @Column(name = "numero_piece", nullable = false, unique = true, length = 60)
    private String numeroPiece;

    /**
     * Champ regie : regie utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regie_id", nullable = false)
    private TresorerieRegieEntity regie;

    /**
     * Champ dateDepense : date depense utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    /**
     * Champ beneficiaire : beneficiaire utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @Column(nullable = false, length = 150)
    private String beneficiaire;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 255)
    private String motif;

    /**
     * Champ imputationBudgetaire : imputation budgetaire utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @Column(name = "imputation_budgetaire", length = 100)
    private String imputationBudgetaire;

    /**
     * Valeur financière (montant depense fcfa) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_depense_fcfa", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDepenseFcfa;

    /**
     * Champ justificatifRef : justificatif ref utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @Column(name = "justificatif_ref", length = 100)
    private String justificatifRef;

    /**
     * Champ justifie : justifie utilisé pour le pilotage et le suivi dans TresorerieRegieDepenseEntity.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean justifie = false;
}
