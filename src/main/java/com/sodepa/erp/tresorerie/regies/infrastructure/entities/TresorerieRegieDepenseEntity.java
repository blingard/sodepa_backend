package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tresorerie_regie_depenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieDepenseEntity {

    @Id
    private UUID id;

    @Column(name = "numero_piece", nullable = false, unique = true, length = 60)
    private String numeroPiece;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regie_id", nullable = false)
    private TresorerieRegieEntity regie;

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    @Column(nullable = false, length = 150)
    private String beneficiaire;

    @Column(nullable = false, length = 255)
    private String motif;

    @Column(name = "imputation_budgetaire", length = 100)
    private String imputationBudgetaire;

    @Column(name = "montant_depense_fcfa", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDepenseFcfa;

    @Column(name = "justificatif_ref", length = 100)
    private String justificatifRef;

    @Column(nullable = false)
    @Builder.Default
    private boolean justifie = false;
}
