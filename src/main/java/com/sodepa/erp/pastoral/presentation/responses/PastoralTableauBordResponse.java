package com.sodepa.erp.pastoral.presentation.responses;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Composant applicatif `PastoralTableauBordResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralTableauBordResponse {

    private long totalTetesVivantes;
    private BigDecimal valeurEstimeeTotaleCheptel;
    private long totalNaissancesExercice;
    private long totalMortalitesExercice;
    private long totalVentesAbattagesExercice;

    private List<RanchStatResponse> ranchesStats;
    private Map<String, Long> repartitionParCategorie;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RanchStatResponse {
        private String ranchNom;
        private String region;
        private long effectifActuel;
        private int capaciteTetes;
        private BigDecimal tauxOccupationPourcent;
        private BigDecimal regiePlafondFcfa;
    }
}
