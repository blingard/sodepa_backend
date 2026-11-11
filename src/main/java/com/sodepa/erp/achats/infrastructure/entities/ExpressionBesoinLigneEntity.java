package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `ExpressionBesoinLigneEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "achat_expression_besoin_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpressionBesoinLigneEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "EBL";

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
     * Champ expressionBesoin : expression besoin utilisé pour le pilotage et le suivi dans ExpressionBesoinLigneEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_besoin_id", nullable = false)
    private ExpressionBesoinEntity expressionBesoin;

    /**
     * Champ ordre : ordre utilisé pour le pilotage et le suivi dans ExpressionBesoinLigneEntity.
     */
    @Column(nullable = false)
    private Integer ordre;

    /**
     * Désignation complète du produit, service ou élément dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 255)
    private String designation;

    /**
     * Champ specificationsTechniques : specifications techniques utilisé pour le pilotage et le suivi dans ExpressionBesoinLigneEntity.
     */
    @Column(name = "specifications_techniques", columnDefinition = "TEXT")
    private String specificationsTechniques;

    /**
     * Quantité ou volume exprimé dans l'unité de mesure de référence dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite;

    /**
     * Unité de mesure réglementaire dans le cadre des opérations SODEPA.
     */
    @Column(name = "unite_mesure", length = 30)
    @Builder.Default
    private String uniteMesure = "U";

    /**
     * Valeur financière (prix unitaire estime) exprimée en Francs CFA (XAF).
     */
    @Column(name = "prix_unitaire_estime", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal prixUnitaireEstime = BigDecimal.ZERO;

    /**
     * Valeur financière (montant total estime) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_total_estime", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantTotalEstime = BigDecimal.ZERO;
}
