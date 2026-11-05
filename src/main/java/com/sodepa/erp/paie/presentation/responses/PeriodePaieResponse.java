package com.sodepa.erp.paie.presentation.responses;

import com.sodepa.erp.paie.infrastructure.entities.StatutPeriodePaie;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodePaieResponse {

    private UUID id;
    private Integer mois;
    private Integer annee;
    private String code;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutPeriodePaie statut;
    private BigDecimal masseSalarialeNette;
    private BigDecimal masseSalarialeBrute;
    private BigDecimal totalChargesPatronales;
    private BigDecimal coutGlobalSodepa;
    private int nombreBulletins;
    private Instant dateCalcul;
    private Instant dateValidation;
    private Instant dateCloture;
}
