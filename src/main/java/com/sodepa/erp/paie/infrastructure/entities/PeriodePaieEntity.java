package com.sodepa.erp.paie.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité de persistance représentant `PeriodePaieEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "paie_periodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodePaieEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ mois : mois utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(nullable = false)
    private Integer mois;

    /**
     * Champ annee : annee utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(nullable = false)
    private Integer annee;

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPeriodePaie statut = StatutPeriodePaie.OUVERTE;

    /**
     * Champ masseSalarialeNette : masse salariale nette utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(name = "masse_salariale_nette", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal masseSalarialeNette = BigDecimal.ZERO;

    /**
     * Champ masseSalarialeBrute : masse salariale brute utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(name = "masse_salariale_brute", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal masseSalarialeBrute = BigDecimal.ZERO;

    /**
     * Champ totalChargesPatronales : total charges patronales utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(name = "total_charges_patronales", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalChargesPatronales = BigDecimal.ZERO;

    /**
     * Valeur financière (cout global sodepa) exprimée en Francs CFA (XAF).
     */
    @Column(name = "cout_global_sodepa", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal coutGlobalSodepa = BigDecimal.ZERO;

    /**
     * Champ dateCalcul : date calcul utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(name = "date_calcul")
    private Instant dateCalcul;

    /**
     * Date et heure de validation hiérarchique dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_validation")
    private Instant dateValidation;

    /**
     * Champ dateCloture : date cloture utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @Column(name = "date_cloture")
    private Instant dateCloture;

    /**
     * Champ bulletins : bulletins utilisé pour le pilotage et le suivi dans PeriodePaieEntity.
     */
    @OneToMany(mappedBy = "periode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BulletinPaieEntity> bulletins = new ArrayList<>();
}
