package com.sodepa.erp.paie.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "paie_bulletin_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinLigneEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bulletin_id", nullable = false)
    private BulletinPaieEntity bulletin;

    @Column(nullable = false)
    private Integer ordre;

    @Column(name = "code_rubrique", nullable = false, length = 30)
    private String codeRubrique;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensRubrique sens;

    @Column(name = "base_calcul", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal baseCalcul = BigDecimal.ZERO;

    @Column(precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taux = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal gain = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal retenue = BigDecimal.ZERO;

    @Column(name = "charge_patronale", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal chargePatronale = BigDecimal.ZERO;
}
