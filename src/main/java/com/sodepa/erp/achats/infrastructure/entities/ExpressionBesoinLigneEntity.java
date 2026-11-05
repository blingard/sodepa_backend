package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "achat_expression_besoin_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpressionBesoinLigneEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_besoin_id", nullable = false)
    private ExpressionBesoinEntity expressionBesoin;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false, length = 255)
    private String designation;

    @Column(name = "specifications_techniques", columnDefinition = "TEXT")
    private String specificationsTechniques;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite;

    @Column(name = "unite_mesure", length = 30)
    @Builder.Default
    private String uniteMesure = "U";

    @Column(name = "prix_unitaire_estime", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal prixUnitaireEstime = BigDecimal.ZERO;

    @Column(name = "montant_total_estime", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantTotalEstime = BigDecimal.ZERO;
}
