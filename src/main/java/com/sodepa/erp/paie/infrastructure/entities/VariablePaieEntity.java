package com.sodepa.erp.paie.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "paie_variables_mensuelles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariablePaieEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periode_id", nullable = false)
    private PeriodePaieEntity periode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    @Column(name = "code_rubrique", nullable = false, length = 30)
    private String codeRubrique;

    @Column(length = 150)
    private String libelle;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantite = BigDecimal.ONE;

    @Column(length = 255)
    private String commentaire;
}
