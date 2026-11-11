package com.sodepa.erp.achats.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité de persistance représentant `ExpressionBesoinEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "achat_expressions_besoin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpressionBesoinEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "EB";

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
     * Champ numero : numero utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    /**
     * Agent ou responsable ayant initié la demande dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id")
    private EmployeEntity demandeur;

    /**
     * Nom ou désignation textuelle de demandeur pour l'affichage et la traçabilité.
     */
    @Column(name = "demandeur_nom", nullable = false, length = 150)
    private String demandeurNom;

    /**
     * Champ uniteOrganisationnelle : unite organisationnelle utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Nom ou désignation textuelle de structure pour l'affichage et la traçabilité.
     */
    @Column(name = "structure_nom", length = 150)
    private String structureNom;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 255)
    private String motif;

    /**
     * Champ dateExpression : date expression utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @Column(name = "date_expression", nullable = false)
    private LocalDate dateExpression;

    /**
     * Valeur financière (montant estime total) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_estime_total", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantEstimeTotal = BigDecimal.ZERO;

    /**
     * Champ stockDisponible : stock disponible utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @Column(name = "stock_disponible", nullable = false)
    @Builder.Default
    private boolean stockDisponible = false;

    /**
     * Champ attestationCarenceEtablie : attestation carence etablie utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @Column(name = "attestation_carence_etablie", nullable = false)
    @Builder.Default
    private boolean attestationCarenceEtablie = true;

    /**
     * Champ budgetDisponible : budget disponible utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @Column(name = "budget_disponible", nullable = false)
    @Builder.Default
    private boolean budgetDisponible = true;

    /**
     * Champ ligneBudgetaireImputation : ligne budgetaire imputation utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @Column(name = "ligne_budgetaire_imputation", length = 100)
    private String ligneBudgetaireImputation;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutExpressionBesoin statut = StatutExpressionBesoin.BROUILLON;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    /**
     * Champ lignes : lignes utilisé pour le pilotage et le suivi dans ExpressionBesoinEntity.
     */
    @OneToMany(mappedBy = "expressionBesoin", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<ExpressionBesoinLigneEntity> lignes = new ArrayList<>();
}
