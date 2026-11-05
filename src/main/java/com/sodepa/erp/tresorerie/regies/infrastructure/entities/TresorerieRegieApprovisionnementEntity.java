package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tresorerie_regie_approvisionnements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieApprovisionnementEntity {

    @Id
    private UUID id;

    @Column(name = "numero_demande", nullable = false, unique = true, length = 60)
    private String numeroDemande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regie_id", nullable = false)
    private TresorerieRegieEntity regie;

    @Column(name = "montant_demande_fcfa", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDemandeFcfa;

    @Column(name = "solde_avant_approv", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeAvantApprov = BigDecimal.ZERO;

    @Column(name = "motif_reapprovisionnement", nullable = false, length = 255)
    private String motifReapprovisionnement;

    @Column(name = "etat_justificatifs_depenses", columnDefinition = "TEXT")
    private String etatJustificatifsDepenses;

    @Column(name = "montant_justificatifs_valides", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantJustificatifsValides = BigDecimal.ZERO;

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutApprovisionnementRegie statut = StatutApprovisionnementRegie.EN_ATTENTE_DAF;

    @Column(name = "signature_daf", nullable = false)
    @Builder.Default
    private boolean signatureDaf = false;

    @Column(name = "date_signature_daf")
    private Instant dateSignatureDaf;

    @Column(name = "signature_dg", nullable = false)
    @Builder.Default
    private boolean signatureDg = false;

    @Column(name = "date_signature_dg")
    private Instant dateSignatureDg;

    @Column(name = "reference_virement_banque", length = 100)
    private String referenceVirementBanque;

    @Column(name = "date_effet_deblocage")
    private LocalDate dateEffetDeblocage;
}
