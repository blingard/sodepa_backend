package com.sodepa.erp.paie.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "paie_periodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodePaieEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Integer mois;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPeriodePaie statut = StatutPeriodePaie.OUVERTE;

    @Column(name = "masse_salariale_nette", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal masseSalarialeNette = BigDecimal.ZERO;

    @Column(name = "masse_salariale_brute", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal masseSalarialeBrute = BigDecimal.ZERO;

    @Column(name = "total_charges_patronales", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalChargesPatronales = BigDecimal.ZERO;

    @Column(name = "cout_global_sodepa", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal coutGlobalSodepa = BigDecimal.ZERO;

    @Column(name = "date_calcul")
    private Instant dateCalcul;

    @Column(name = "date_validation")
    private Instant dateValidation;

    @Column(name = "date_cloture")
    private Instant dateCloture;

    @OneToMany(mappedBy = "periode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BulletinPaieEntity> bulletins = new ArrayList<>();
}
